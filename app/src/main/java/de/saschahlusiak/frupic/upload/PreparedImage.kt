package de.saschahlusiak.frupic.upload

import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.io.path.name
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

data class PreparedImage(
    val name: String,
    val path: String,
    val size: Long,
    val width: Int,
    val height: Int
) {
    val file get() = File(path)

    fun delete() {
        file.delete()
    }
}

private const val tag = "PreparedImage"

/**
 * Resizes given input file and saves result into output file
 *
 * @param minDimension the longest edge will be at least this big
 * @param quality jpeg quality
 *
 * @return Resized instance
 */
suspend fun PreparedImage.resized(
    minDimension: Int = 1024,
    quality: Int = 85
): PreparedImage? = withContext(Dispatchers.Default) {
    /* this will get the original image size in px */
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, options)

    options.inSampleSize = 1

    /* scale image down to the smallest power of 2 that will fit into the desired dimensions */
    if (options.outHeight * options.outWidth * 2 >= 16384) {
        val scaleByHeight =
            abs(options.outHeight - minDimension) >= abs(options.outWidth - minDimension)

        val sampleSize = if (scaleByHeight)
            options.outHeight.toFloat() / minDimension.toFloat()
        else
            options.outWidth.toFloat() / minDimension.toFloat()

        options.inSampleSize = 2.0.pow(floor(ln(sampleSize) / ln(2.0f).toDouble())).toInt()

        Log.d(
            tag,
            "Original(${options.outWidth}x${options.outHeight} -> inSampleSize = ${options.inSampleSize}"
        )
    }

    options.inJustDecodeBounds = false

    /* get a scaled version of our original image */
    val b = BitmapFactory.decodeFile(path, options) ?: return@withContext null

    val file = File(file.parentFile, UUID.randomUUID().toString())
    file.outputStream().use { stream ->
        b.compress(CompressFormat.JPEG, quality, stream)
    }

    return@withContext PreparedImage(
        name = name,
        path = file.absolutePath,
        size = file.length(),
        width = options.outWidth,
        height = options.outHeight
    )
}