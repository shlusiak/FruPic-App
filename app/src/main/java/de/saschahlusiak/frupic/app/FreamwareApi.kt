package de.saschahlusiak.frupic.app

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

typealias OnDownloadProgressListener = (frupic: Frupic, copied: Int, max: Int) -> Unit

/**
 * Wrapper for API calls to https://api.freamware.net or related.
 *
 * - [getPicture] retrieve list of pictures (index)
 * - [downloadFrupic] download the given frupic full picture
 */
class FreamwareApi @Inject constructor() {
    /**
     * Fetches the list of available Frupics for the given window.
     *
     * @return list of [Frupic]
     */
    @Throws(JSONException::class)
    suspend fun getPicture(offset: Int, limit: Int): List<Frupic> {
        val query = "${INDEX_URL}?offset=$offset&limit=$limit"
        val json = get(query)
        return JSONArray(json).toList<JSONObject>().map { jo ->
            Frupic(
                jo.getInt("id"),
                Frupic.FLAG_NEW or Frupic.FLAG_UNSEEN,
                jo.getString("url"),
                jo.getString("thumb_url"),
                jo.getString("date"),
                jo.getString("username"),
                jo.getJSONArray("tags").toList()
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
    suspend fun downloadFrupic(frupic: Frupic, target: File, listener: OnDownloadProgressListener? = null) {
        target.delete();

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
                    listener?.invoke(frupic, copied, total)
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

                                yield()
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

    companion object {
        private const val INDEX_URL = "https://api.freamware.net/2.0/get.picture"
    }
}