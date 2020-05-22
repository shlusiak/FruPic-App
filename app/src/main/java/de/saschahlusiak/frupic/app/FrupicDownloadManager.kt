package de.saschahlusiak.frupic.app

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.Collections.synchronizedMap
import javax.inject.Inject

sealed class Result {
    class Success : Result()
    class Cancelled : Result()
    class Failed : Result()
}

class DownloadJob(
    val frupic: Frupic
) {
    var job: Job? = null
    val progress = MutableLiveData<Pair<Int, Int>>()
    val result = MutableLiveData<Result>()
}

class FrupicDownloadManager @Inject constructor(
    private val store: FrupicStorage,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val allJobs: MutableMap<Frupic, DownloadJob> = synchronizedMap(mutableMapOf<Frupic, DownloadJob>())
    private val channel = Channel<DownloadJob>()

    private val concurrentDownloads = 3

    init {
        repeat(concurrentDownloads) {
            scope.launch {
                for (job in channel) {
                    coroutineScope {
                        // above scope will block until the job complete
                        // but every job can be cancelled individually
                        job.job = launch { processJob(job) }
                    }
                }
            }
        }
    }

    private suspend fun processJob(job: DownloadJob){
        val frupic = job.frupic

        // skip if we have received work that is already been cancelled
        if (!allJobs.containsKey(frupic)) return

        Log.d(tag, "Starting job #${frupic.id}")
        try {
            val file = store.download(frupic) { copied, max ->
                job.progress.value = copied to max
            }
            Log.d(tag, "Job #${frupic.id} success, downloaded to ${file.absolutePath}")
            job.result.value = Result.Success()
        }
        catch (e: CancellationException) {
            e.printStackTrace()
            Log.d(tag, "Job #${frupic.id} cancelled")
            job.result.value = Result.Cancelled()
        }
        catch (e: Exception) {
            crashlytics.recordException(e)
            e.printStackTrace()
            analytics.logEvent("frupic_download_failed", null)

            Log.d(tag, "Job #${frupic.id} failed")
            job.result.value = Result.Failed()
        }
        finally {
            allJobs.remove(frupic)
        }
    }

    fun enqueue(job: DownloadJob): Boolean {
        val frupic = job.frupic
        if (allJobs.containsKey(frupic)) return true
        allJobs[frupic] = job

        scope.launch {
            Log.d(tag, "Enqueuing job #${frupic.id}")
            channel.send(job)
        }

        return true
    }

    fun getJob(frupic: Frupic) = allJobs[frupic]

    fun cancel(frupic: Frupic): Boolean {
        val job = allJobs[frupic] ?: return false

        Log.d(tag, "Cancelling job #${frupic.id}")

        allJobs.remove(frupic)
        job.job?.cancel()

        return true
    }

    private fun cancelAllJobs() {
        val keys = allJobs.keys.toList()
        for (frupic in keys) cancel(frupic)
    }

    fun shutdown() {
        cancelAllJobs()
        // this instance is now invalid
        scope.cancel()
    }

    companion object {
        private val tag = FrupicDownloadManager::class.simpleName
    }
}