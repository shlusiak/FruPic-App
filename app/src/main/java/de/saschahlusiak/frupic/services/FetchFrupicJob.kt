package de.saschahlusiak.frupic.services

import android.util.Log
import androidx.annotation.WorkerThread
import de.saschahlusiak.frupic.app.FrupicManager
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.runBlocking

@Deprecated("")
class FetchFrupicJob constructor(val frupic: Frupic, private val manager: FrupicManager) : Job() {
    public override fun run(): JobState {
        Log.d(Companion.tag, "fetching #" + frupic.id)
        if (!doFetch()) {
            Log.d(Companion.tag, "failed #" + frupic.id)
            return JobState.JOB_FAILED
        }
        Log.d(Companion.tag, "fetched #" + frupic.id)
        return JobState.JOB_SUCCESS
    }

    /**
     * @return true on success, false otherwise
     */
    @WorkerThread
    fun doFetch(): Boolean {
        return try {
            runBlocking {
                manager.download(frupic) { _, copied, max ->
                    synchronized(jobListener) {
                        for (l in jobListener) {
                            l.OnJobProgress(this@FetchFrupicJob, copied, max)
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private val tag = FetchFrupicJob::class.java.simpleName
    }
}