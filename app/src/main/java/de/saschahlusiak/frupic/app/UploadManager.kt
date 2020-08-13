package de.saschahlusiak.frupic.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import de.saschahlusiak.frupic.upload.UploadService
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

data class PreparedImage(
    val name: String,
    val original: Image,
    val resized: Deferred<Image?>
) {
    class Image(
        val path: String,
        val size: Long,
        val width: Int,
        val height: Int
    ): Serializable {
        val file get() = File(path)

        fun delete() {
            file.delete()
        }
    }

    suspend fun delete() {
        original.delete()
        resized.await()?.delete()
    }
}

internal data class UploadJob(
    val username: String,
    val tags: String,
    val file: File
) {
    fun delete() {
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

    /**
     * Copies the list of Uris into internal storage and creates resized versions of it.
     * 
     * The returned [PreparedImage.Image] instances are not owned by the UploadManager and need to be cleaned up
     * by the caller if not submitted.
     *
     * @see [PreparedImage.Image.delete]
     * @see submit
     */
    suspend fun prepareForUpload(uris: List<Uri>): List<PreparedImage> {
        return withContext(Dispatchers.Default) {
            uris.mapNotNull { prepareForUpload(it) }
        }
    }

    private fun prepareForUpload(uri: Uri): PreparedImage? {
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
        val original = PreparedImage.Image(tempFile.absolutePath, size, options.outWidth, options.outHeight)

        val resized = GlobalScope.async {
            val result = resize(original, quality = 90)
            result ?: return@async null
            // if scaled image is > 1M, resize again with lower quality
            if (result.size > 1024*1024) {
                result.delete()
                resize(original, quality = 75)
            } else {
                result
            }
        }

        return PreparedImage(
            name = name ?: "[Unknown]",
            original = original,
            resized = resized
        )
    }

    /**
     * Sends the given jobs to the [UploadService].
     */
    fun submit(images: List<PreparedImage.Image>, username: String, tags: String) {
        for (image in images) {
            Log.d(tag, "Submitting to service: ${image.path}")
            val intent = Intent(context, UploadService::class.java)

            intent.putExtra("username", username)
            intent.putExtra("tags", tags)
            intent.putExtra("path", image.path)

            context.startService(intent)
        }
    }

    /**
     * Resizes given input file and saves result into output file
     *
     * @param input input file. Must exist
     * @param minDimension the longest edge will be at least this big
     *
     * @return Resized instance
     */
    private suspend fun resize(input: PreparedImage.Image, minDimension: Int = 1024, quality: Int = 90): PreparedImage.Image? = withContext(Dispatchers.IO) {
        /* this will get the original image size in px */
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(input.path, options)

        options.inSampleSize = 1

        /* scale image down to the smallest power of 2 that will fit into the desired dimensions */
        if (options.outHeight * options.outWidth * 2 >= 16384) {
            val scaleByHeight = abs(options.outHeight - minDimension) >= abs(options.outWidth - minDimension)

            val sampleSize = if (scaleByHeight)
                options.outHeight.toFloat() / minDimension.toFloat()
            else
                options.outWidth.toFloat() / minDimension.toFloat()

            options.inSampleSize = 2.0.pow(floor(ln(sampleSize) / ln(2.0f).toDouble())).toInt()

            Log.d(tag, "Original(${options.outWidth}x${options.outHeight} -> inSampleSize = ${options.inSampleSize}")
        }

        options.inJustDecodeBounds = false

        /* get a scaled version of our original image */
        val b = BitmapFactory.decodeFile(input.path, options) ?: return@withContext null

        val file = File(tempDir, UUID.randomUUID().toString())
        file.outputStream().use { stream ->
            b.compress(CompressFormat.JPEG, quality, stream)
        }

        return@withContext PreparedImage.Image(
            path = file.absolutePath,
            size = file.length(),
            width = options.outWidth,
            height = options.outHeight
        )
    }
}