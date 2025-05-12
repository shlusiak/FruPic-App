package de.saschahlusiak.frupic.app.job

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SynchronizeJob : JobService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            val before = repository.getFrupicCount(Frupic.FLAG_NEW)
            if (!repository.synchronize()) {
                jobFinished(params, true)
                return@launch
            }

            val after = repository.getFrupicCount(Frupic.FLAG_NEW)

            if (after == 0) {
                notificationManager.clearUnseenNotification()
            } else if (after != before) {
                notificationManager.updateUnseenNotification(after)
            }
            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        private val tag = SynchronizeJob::class.simpleName
        private const val JOB_ID = 2
        private const val INTERVAL_HOURS = 4
        private const val INTERVAL_MILLIS = INTERVAL_HOURS * 60 * 60 * 1000L

        /**
         * Schedule this job repeatedly, every [INTERVAL_HOURS].
         *
         * Call [unschedule] to unschedule.
         */
        fun schedule(context: Context) {
            Log.d(tag, "Scheduling job")

            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, SynchronizeJob::class.java)).apply {
                setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                setPeriodic(INTERVAL_MILLIS)
                setPersisted(true)
            }.build()

            js.schedule(jobInfo)
        }

        /**
         * Removes the currently scheduled job
         */
        fun unschedule(context: Context) {
            Log.d(tag, "Unscheduling job")
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            js.cancel(JOB_ID)
        }
    }
}