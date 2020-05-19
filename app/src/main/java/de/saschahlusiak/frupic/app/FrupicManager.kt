package de.saschahlusiak.frupic.app

import android.content.Context
import android.util.Log
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

/**
 * Manages downloads of fully downloaded Frupics.
 */
class FrupicManager @Inject constructor(
    context: Context,
    private val api: FreamwareApi
) {
    private val tag = FrupicManager::class.simpleName
    private val cacheDir: File = context.externalCacheDir ?: context.cacheDir

    init {
        Log.d(tag, "Initializing...")

        cacheDir.mkdirs()
    }

    private val Frupic.filename: String get() = File(fullUrl).name

    /**
     * Return the [File] where to find the Frupic if downloaded. May not exist.
     * @return file for Frupic once downloaded. May not exist.
     */
    fun getFile(frupic: Frupic): File {
        return File(cacheDir, frupic.filename)
    }

    /**
     * @return a new temp file to download this Frupic to
     */
    private fun getTempFile(frupic: Frupic): File {
        val tmpDir = File(cacheDir, "tmp")
        tmpDir.mkdirs()
        var tmpFile: File
        do {
            tmpFile = File(tmpDir, frupic.filename + "_" + repeat(10) { Random.nextInt(10) })
        } while (tmpFile.exists())
        return tmpFile
    }

    /**
     * Download given Frupic and return the target File. If file already downloaded, will immediately return target file.
     *
     * Any Exception will bubble up.
     *
     * @param frupic to download
     * @param listener progress listener
     * @return target file
     */
    suspend fun download(frupic: Frupic, listener: OnDownloadProgressListener?): File {
        val target = getFile(frupic)
        val tempFile = getTempFile(frupic)

        if (target.exists()) return target

        api.downloadFrupic(frupic, tempFile, listener)
        target.delete()
        tempFile.renameTo(target)
        return target
    }

    /**
     * Copy the file for the given Frupic to the given destination.
     *
     * Any exception will bubble up.
     *
     * @param frupic the frupic to copy. Must be downloaded already.
     * @param target target file to copy to.
     */
    suspend fun copy(frupic: Frupic, target: File) {
        withContext(Dispatchers.IO) {
            getFile(frupic).copyTo(target, overwrite = true)
        }
    }
}