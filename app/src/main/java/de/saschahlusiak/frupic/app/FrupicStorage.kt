package de.saschahlusiak.frupic.app

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.squareup.picasso.Picasso
import de.saschahlusiak.frupic.app.job.CleanupJob
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Manages download and storage of full Frupics to cache directories.
 */
@Singleton
class FrupicStorage @Inject constructor(
    private val context: Context,
    private val api: FreamwareApi,
    private val picasso: Picasso
) {
    private val tag = FrupicStorage::class.simpleName
    private val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "full")
    private val tmpDir = File(cacheDir, "tmp")

    init {
        Log.d(tag, "Initializing...")

        cacheDir.mkdirs()
        tmpDir.mkdirs()
    }

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
        var tmpFile: File
        do {
            val suffix = Array(10) { Random.nextInt(10).toString() }.joinToString()
            tmpFile = File(tmpDir, frupic.filename + "_" + suffix)
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
    suspend fun download(frupic: Frupic, listener: OnProgressListener?): File {
        val target = getFile(frupic)
        val tempFile = getTempFile(frupic)

        if (target.exists()) return target

        api.downloadFrupic(frupic, tempFile, listener)
        target.delete()
        tempFile.renameTo(target)

        dumpCacheSize()
        return target
    }

    /**
     * Expires old files to reach target cache size
     *
     * @param targetSizeInMb the target cache size on disk in megabytes
     */
    @WorkerThread
    fun maintainCacheSize(targetSizeInMb: Int = 128) {
        val targetSize = targetSizeInMb * 1024 * 1024

        // dump picasso stats, just for good measure
        val stats = picasso.snapshot
        stats.dump()

        // we are not meant to have any tmpFiles at all
        var freed = 0L
        var deleted = 0
        tmpDir.listFiles()?.forEach { file ->
            Log.d(tag, "Deleting tmpFile ${file.name} (${file.length()} bytes)")
            freed += file.length()
            deleted++

            file.delete()
        }

        // all files in cache, sorted by most recent last
        val cacheFiles = cacheDir.listFiles()?.sortedBy { it.lastModified() }?.toMutableList() ?: mutableListOf()
        fun List<File>.totalSize() = sumBy { it.length().toInt() }

        dumpCacheSize()

        while (cacheFiles.totalSize() > targetSize) {
            val file = cacheFiles[0]
            Log.d(tag, "Removing ${file.name} (${file.length() / 1024} kb)")

            deleted++
            freed += file.length()
            cacheFiles.remove(file)

            file.delete()
        }

        dumpCacheSize()
        Log.d(tag, "Cache expiry done, files removed: $deleted (${freed / 1024} kb)")
    }

    private fun dumpCacheSize() {
        val cacheFiles = cacheDir.listFiles()?.sortedBy { it.lastModified() }?.toMutableList() ?: mutableListOf()
        val totalSize = cacheFiles.sumBy { it.length().toInt() }
        Log.d(tag, "Cache size: ${cacheFiles.size} files, ${totalSize / 1024} kb")
    }

    /**
     * Schedule a run of [maintainCacheSize] in the background, at any convenient time
     */
    fun scheduleCacheExpiry() {
        Log.d(tag, "Scheduling cache expiry")
        CleanupJob.schedule(context)
        // TODO: use JobManager to schedule this when device is idle rather than invoking it right now
        GlobalScope.launch(Dispatchers.IO) {
            maintainCacheSize()
        }
    }
}