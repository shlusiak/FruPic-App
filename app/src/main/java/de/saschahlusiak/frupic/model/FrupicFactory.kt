package de.saschahlusiak.frupic.model

import android.content.Context
import androidx.annotation.WorkerThread
import de.saschahlusiak.frupic.app.FreamwareApi
import de.saschahlusiak.frupic.app.FrupicManager
import kotlinx.coroutines.runBlocking
import java.io.File

@Deprecated("Replace with FrupicDownloadManager")
class FrupicFactory(context: Context) {
    private val manager = FrupicManager(context, FreamwareApi())

    @Deprecated("")
    interface OnFetchProgress {
        fun OnProgress(read: Int, length: Int)
    }

    /**
     * Returns a File object for the cached file on storage. File may not exist.
     *
     * @param frupic frupic
     * @return File for cached file
     */
    fun getCacheFile(frupic: Frupic) = manager.getFile(frupic)

    /**
     * Downloads the given image into file cache.
     *
     * @param frupic
     * @param progress
     * @return true on success, false otherwise
     */
    @WorkerThread
    fun fetchFrupicImage(frupic: Frupic, progress: OnFetchProgress?): Boolean {
        return try {
            runBlocking {
                manager.download(frupic) { _, copied, max ->
                    progress?.OnProgress(copied, max)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun copyImageFile(frupic: Frupic, target: File): Boolean {
        return try {
            runBlocking {
                manager.copy(frupic, target)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}