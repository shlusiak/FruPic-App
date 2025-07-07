package de.saschahlusiak.frupic.upload

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

@Reusable
class UploadManager @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val tag = UploadManager::class.simpleName
    private val tempDir = File(context.cacheDir, "upload")

    init {
        tempDir.mkdirs()
    }

    /**
     * Copies the list of Uris into internal storage and creates resized versions of it.
     * 
     * The returned [PreparedImage.Image] instances are not owned by the UploadManager and need to be cleaned up
     * by the caller if not submitted.
     *
     * @see submit
     */
    suspend fun prepareForUpload(uris: List<Uri>, resizeIn: CoroutineScope): List<PreparedImage> {
        return withContext(Dispatchers.Default) {
            uris.mapNotNull { prepareForUpload(it, resizeIn) }
        }
    }

    private fun prepareForUpload(uri: Uri, scope: CoroutineScope): PreparedImage? {
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

        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val options = BitmapFactory.Options()
        var b = BitmapFactory.decodeFile(tempFile.absolutePath, options) ?: return null
        if (orientation != 0) {
            // we have to rotate the input image and rewrite the file
            val matrix = Matrix()
            matrix.preRotate(orientation.toFloat())
            b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, matrix, true)

            tempFile.outputStream().use { output ->
                b.compress(CompressFormat.JPEG, 90, output)
            }
        }

        val size = tempFile.length()
        val original = PreparedImage(
            name = name ?: "[Unknown]",
            path = tempFile.absolutePath,
            size = size,
            width = options.outWidth,
            height = options.outHeight
        )

        return original
    }

    /**
     * Sends the given jobs to the [UploadService].
     */
    fun submit(images: List<PreparedImage>, username: String, tags: String) {
        for (image in images) {
            Log.d(tag, "Submitting to service: ${image.path}")
            val intent = Intent(context, UploadService::class.java)

            intent.putExtra("username", username)
            intent.putExtra("tags", tags)
            intent.putExtra("path", image.path)

            context.startService(intent)
        }
    }
}