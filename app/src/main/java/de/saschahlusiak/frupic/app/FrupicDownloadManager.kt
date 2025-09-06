package de.saschahlusiak.frupic.app

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.Collections.synchronizedMap
import javax.inject.Inject

sealed interface JobStatus {
    data object Scheduled : JobStatus
    data class InProgress(val progress: Long, val max: Long) : JobStatus
    data class Success(val file: File) : JobStatus
    data object Cancelled : JobStatus
    data object Failed : JobStatus
}

class DownloadJob(
    val frupic: Frupic
) {
    var job: Job? = null
    val status = MutableStateFlow<JobStatus>(JobStatus.Scheduled)
}

class FrupicDownloadManager @Inject constructor(
    private val storage: FrupicStorage,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val allJobs: MutableMap<Frupic, DownloadJob> =
        synchronizedMap(mutableMapOf<Frupic, DownloadJob>())
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

    private suspend fun processJob(job: DownloadJob) {
        val frupic = job.frupic

        // skip if we have received work that is already been cancelled
        if (!allJobs.containsKey(frupic)) return

        Log.d(tag, "Starting job #${frupic.id}")
        try {
            val file = storage.download(frupic) { copied, max ->
                job.status.value = JobStatus.InProgress(copied, max)
            }
            Log.d(tag, "Job #${frupic.id} success, downloaded to ${file.absolutePath}")
            job.status.value = JobStatus.Success(file)
        } catch (e: CancellationException) {
            e.printStackTrace()
            Log.d(tag, "Job #${frupic.id} cancelled")
            job.status.value = JobStatus.Cancelled
        } catch (e: Exception) {
            crashlytics.recordException(e)
            e.printStackTrace()
            analytics.logEvent("frupic_download_failed", null)

            Log.d(tag, "Job #${frupic.id} failed")
            job.status.value = JobStatus.Failed
        } finally {
            allJobs.remove(frupic)
        }
    }

    private fun enqueue(job: DownloadJob) {
        val frupic = job.frupic
        if (allJobs.containsKey(frupic)) return
        allJobs[frupic] = job

        scope.launch {
            Log.d(tag, "Enqueuing job #${frupic.id}")
            channel.send(job)
        }
    }

    fun getJob(frupic: Frupic): DownloadJob {
        val existing = allJobs[frupic]
        if (existing != null) return existing

        val file = storage.getFile(frupic)
        if (file.exists()) {
            return DownloadJob(frupic).apply {
                status.value = JobStatus.Success(file)
            }
        }

        return DownloadJob(frupic).also {
            enqueue(it)
        }
    }

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

        storage.scheduleCacheExpiry()
    }

    companion object {
        private val tag = FrupicDownloadManager::class.simpleName
    }
}