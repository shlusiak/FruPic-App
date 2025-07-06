package de.saschahlusiak.frupic.app

import android.util.Log
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.model.cloudfront
import de.saschahlusiak.frupic.utils.toList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

typealias OnProgressListener = (copied: Int, max: Int) -> Unit

/**
 * Wrapper for API calls to https://api.freamware.net or related.
 *
 * - [getPicture] retrieve list of pictures (index)
 * - [downloadFrupic] download the given frupic full picture
 * - [uploadImage] upload image data
 */
class FreamwareApi @Inject constructor() {
    private val tag = FreamwareApi::class.simpleName

    /**
     * Fetches the list of available Frupics for the given window.
     *
     * @return list of [Frupic]
     */
    @Throws(JSONException::class)
    suspend fun getPicture(offset: Int, limit: Int): List<Frupic> {
        val query = "${GET_PICTURE_ENDPOINT}?offset=$offset&limit=$limit"
        val json = get(query)

        return JSONArray(json).toList<JSONObject>().map { jo ->
            Frupic(
                id = jo.getInt("id"),
                isStarred = false,
                isNew = true,
                fullUrl = jo.getString("url"),
                thumbUrl = jo.getString("thumb_url"),
                date = jo.getString("date"),
                username = jo.getString("username"),
                tagsString = jo.getJSONArray("tags").toList<String>().joinToString(", ")
            )
        }
    }

    /**
     * Return the response of the given URL as String.
     *
     * Runs on the IO dispatcher.
     */
    @Throws(IOException::class)
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        return@withContext URL(url).openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use {
                it.readLines().joinToString("\n")
            }
        }
    }

    /**
     * Downloads the given Frupic image into the given file. Any exception is being thrown up.
     *
     * @param frupic the Frupic to fetch
     * @param target Target file. This should be a temp file and on success should be moved into the final position.
     * @param listener optional progress listener
     *
     * @return true on success, false otherwise
     */
    suspend fun downloadFrupic(frupic: Frupic, target: File, listener: OnProgressListener? = null) {
        target.delete()

        val url = URL(frupic.fullUrl.cloudfront)
        val (connection, total) = withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            val total = connection.contentLength
            connection to total
        }

        coroutineScope {
            val progress = Channel<Int>(capacity = Channel.CONFLATED)

            launch {
                for (copied in progress) {
                    listener?.invoke(copied, total)
                }
            }

            withContext(Dispatchers.IO) {
                try {
                    connection.inputStream.use { inputStream ->
                        FileOutputStream(target).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var copied = 0

                            do {
                                read = inputStream.read(buffer)
                                if (read <= 0) break

                                outputStream.write(buffer, 0, read)
                                copied += read

                                progress.send(copied)
                            } while (true)
                        }
                    }

                    true
                } catch (e: Exception) {
                    target.delete()
                    e.printStackTrace()
                    throw e
                } finally {
                    connection.disconnect()
                    progress.close()
                }
            }
        }
    }

    suspend fun uploadImage(source: InputStream, username: String, tags: String, listener: OnProgressListener?) {
        val imageData = source.use { it.readBytes() }

        if (false && BuildConfig.DEBUG) {
            // TODO: FOR DEBUG
            Log.w(tag, "FAKE UPLOAD!!! NOT SENDING TO FRUPIC!!")
            delay(1000)
            (1..100).forEach {
                listener?.invoke(it, 100)
                delay(50)
            }
            return
        }

        val lineEnd = "\r\n"
        val twoHyphens = "--"
        val boundary = "ForeverFrubarIWantToBe"

        // Tags
        var header = lineEnd + twoHyphens + boundary + lineEnd +
            "Content-Disposition: form-data;name='tags';" +
            lineEnd + lineEnd + tags + ";" + lineEnd +
            lineEnd + twoHyphens + boundary + lineEnd

        if (username != "") {
            header += lineEnd + twoHyphens + boundary + lineEnd
            header += "Content-Disposition: form-data;name='username';"
            header += lineEnd + lineEnd + username + ";" + lineEnd +
                lineEnd + twoHyphens + boundary + lineEnd
        }
        val filename = "frup0rn.png"
        val footer = lineEnd + twoHyphens + boundary + twoHyphens + lineEnd
        val size = imageData.size

        Log.d(tag, "Connecting to ${UPLOAD_PICTURE_ENDPOINT} for upload")
        val connection = withContext(Dispatchers.IO) {
            val conn = URL(UPLOAD_PICTURE_ENDPOINT).openConnection() as HttpURLConnection
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")


            header += "Content-Disposition: form-data;name='file';filename='$filename'$lineEnd$lineEnd"

            conn.setFixedLengthStreamingMode(header.length + footer.length + imageData.size)

            Log.d(tag, "Connected")

            return@withContext conn
        }

        coroutineScope {
            val progress = Channel<Int>(capacity = Channel.CONFLATED)

            launch {
                for (copied in progress) {
                    listener?.invoke(copied, size)
                }
            }

            // send
            launch(Dispatchers.IO) {
                Log.d(tag, "Uploading image")
                Log.d(tag, "Headers: $header")

                try {
                    val dataStream = ByteArrayInputStream(imageData)
                    val data = ByteArray(16384)
                    var nRead: Int
                    var written = 0

                    connection.outputStream.use { output ->
                        val dos = DataOutputStream(output)
                        dos.writeBytes(header)

                        while (dataStream.read(data, 0, data.size).also { nRead = it } != -1) {
                            dos.write(data, 0, nRead)
                            written += nRead
                            dos.flush()
                            progress.send(written)
                            yield()
                        }

                        dos.writeBytes(footer)
                        dos.close()

                        // read response
                        Log.d(tag, "response = ${connection.responseCode}: {${connection.responseMessage}")

                        val lines = connection.inputStream.use { input ->
                            BufferedReader(InputStreamReader(input)).use { reader ->
                                reader.readLines()
                            }
                        }

                        Log.d(tag, "Server responded with: $lines")
                    }
                } finally {
                    progress.close()
                    connection.disconnect()
                }
            }
        }
    }


    companion object {
        private const val GET_PICTURE_ENDPOINT = "https://api.freamware.net/2.0/get.picture"
        private const val UPLOAD_PICTURE_ENDPOINT = "https://api.freamware.net/2.0/upload.picture"
    }
}