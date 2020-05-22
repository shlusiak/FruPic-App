package de.saschahlusiak.frupic.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import de.saschahlusiak.frupic.upload.UploadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable
import java.util.*
import javax.inject.Inject

data class UploadImage(
    val orientation: Int,
    val name: String,
    val size: Long,
    val path: String
): Serializable {
    val file get() = File(path)

    fun cleanup() {
        file.delete()
    }
}

class UploadManager @Inject constructor(
    val context: Context
) {
    private val tag = UploadManager::class.simpleName
    private val tempDir = File(context.cacheDir, "upload")

    init {
        tempDir.mkdirs()
    }

    suspend fun prepareForUpload(uris: List<Uri>): List<UploadImage> {
        return withContext(Dispatchers.Default) {
            uris.map { prepareFile(it) }
        }
    }

    private fun prepareFile(uri: Uri): UploadImage {
        var orientation = 0
        var name = uri.lastPathSegment
        Log.d(tag, "Copying ${uri.path}")

        /* first get the orientation for the image, necessary when scaling the image,
		 * so the orientation is preserved */
        try {
            val columns = arrayOf(
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val cursor = context.contentResolver.query(
                uri,
                columns,
                null, null, null
            )
            if (cursor != null) {

                if (cursor.count == 1) {
                    cursor.moveToFirst()
                    orientation = cursor.getInt(0)
                    name = cursor.getString(1)
                }

                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tempFile = File(tempDir, UUID.randomUUID().toString())

        val size = context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return UploadImage(orientation, name ?: "[Unknown]", size ?: 0, tempFile.absolutePath)
    }

    /**
     * Sends the given jobs to the [UploadService].
     */
    fun submit(images: List<UploadImage>, resize: Boolean, username: String, tags: String) {
        for (image in images) {
            Log.d(tag, "Submitting to service: ${image.path}")
            val intent = Intent(context, UploadService::class.java)

            intent.putExtra("scale", resize)
            intent.putExtra("username", username)
            intent.putExtra("tags", tags)
            intent.putExtra("filename", image.name)
            intent.putExtra("orientation", image.orientation)
            intent.putExtra("path", image.path)

            context.startService(intent)
        }
    }
}