package de.saschahlusiak.frupic.app.job

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Job that is run when the device is idle to clean up old full frupic images in [FrupicStorage].
 */
class CleanupJob : JobService() {
    @Inject
    lateinit var storage: FrupicStorage

    override fun onCreate() {
        super.onCreate()
        (application as App).appComponent.inject(this)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(tag, "onStartJob")

        GlobalScope.launch(Dispatchers.IO) {
            storage.maintainCacheSize()

            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d(tag, "onStopJob")

        return false
    }

    companion object {
        private const val JOB_ID = 1
        private val tag = CleanupJob::class.simpleName

        fun schedule(context: Context) {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, CleanupJob::class.java)).apply {
                setRequiresDeviceIdle(true)
                setPersisted(false)
            }.build()

            js.schedule(jobInfo)
        }
    }
}