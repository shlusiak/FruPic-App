package de.saschahlusiak.frupic.upload

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FreamwareApi
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.UploadJob
import de.saschahlusiak.frupic.grid.GridActivity
import kotlinx.coroutines.*
import java.io.*
import javax.inject.Inject

class UploadService : IntentService("UploadService") {
    private val tag = UploadService::class.java.simpleName

	@Inject
    lateinit var api: FreamwareApi

	@Inject
    lateinit var repository: FrupicRepository

	@Inject
    lateinit var analytics: FirebaseAnalytics

    /**
     * Per running instance of this service we count the number of frupics for the notification.
     *
     * As soon as a batch of jobs finish, this service goes down and the next upload job will reset these counters.
     */

    // how many jobs we have finished processing
    private var current = 0
    // the total number of jobs
    private var max = 0
    // the number of failed jobs so far
    private var failed = 0

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()

        Log.d(tag, "onCreate")
        (applicationContext as App).appComponent.inject(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val intent = Intent(this, GridActivity::class.java)
        pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        max = 0
        current = 0
        failed = 0

        // for the duration of this service we have an ongoing notification
        updateNotification(true, 0.0f)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy")

        updateNotification(false, 0.0f)

        // when we are finished here, schedule a synchronize
        GlobalScope.launch(Dispatchers.Main) {
            repository.synchronize(0, 100)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_UPLOAD, getString(R.string.channel_name_upload), NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun uploadImage(image: InputStream, username: String, tags: String): String? {
        var lastUpdate = 0L

        try {
            api.uploadImage(image, username, tags) { written: Int, size: Int ->
                Log.d(tag, "Progress: $written($size)")
                // we have to rate-limit updates to the notification otherwise the final one may not come through
                if (System.currentTimeMillis() - lastUpdate > 500) {
                    lastUpdate = System.currentTimeMillis()
                    updateNotification(true, written.toFloat() / size.toFloat())
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            return e.message
        }
        return null
    }

    private suspend fun processJob(job: UploadJob) {
        val file = job.file
        val username = job.username
        val tags = job.tags

        /* TODO: watch network state to pause/restart upload */
        val error = file.inputStream().use {
            uploadImage(it, username, tags)
        }

        if (error != null) {
            val bundle = Bundle().apply {
                putString("error", error)
            }
            analytics.logEvent("upload_error", bundle)

            Log.e(tag, "error: $error")

            failed++
            /* TODO: handle error gracefully */
        } else {
            analytics.logEvent("upload_success", null)
            Log.i(tag, "Upload successful: ${file.absolutePath}")
        }

        current++
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand")

        max++
        updateNotification(true, 0.0f)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(tag, "onHandleIntent")
        intent ?: return

        val userName = intent.getStringExtra("username") ?: ""
        val tags = intent.getStringExtra("tags") ?: ""
        val path = intent.getStringExtra("path") ?: ""
        val file = File(path)

        val job = UploadJob(userName, tags, file)
        runBlocking {
            try {
                processJob(job)
            }
            finally {
                job.delete()
            }
        }
    }

    @Synchronized
    private fun updateNotification(ongoing: Boolean, progress: Float) {
        val builder = NotificationCompat.Builder(this, CHANNEL_UPLOAD)

        builder.setContentTitle(getString(R.string.upload_notification_title))
        builder.color = getColor(R.color.brand_yellow_bright)
        if (ongoing) {
            builder.setSmallIcon(R.drawable.frupic_notification_wait)
            builder.setContentText(getString(R.string.upload_notification_progress, current + 1, max))
            if (max > 0) {
                val percent = (current.toFloat() + progress) / max.toFloat()
                builder.setProgress(100, (percent * 100.0f).toInt(), false)
            }
            builder.setAutoCancel(false)
            builder.setOngoing(true)
            /* TODO: provide intent to see progress dialog and support for cancel */
        } else {
            if (failed == 0) {
                builder.setContentTitle(getString(R.string.upload_notification_title_finished))
                builder.setSmallIcon(R.drawable.frupic_notification_success)
                builder.setContentText(getString(R.string.upload_notification_success, max))
                builder.setTicker(getString(R.string.upload_notification_success, max))
            } else {
                builder.setContentTitle(getString(R.string.upload_notification_title_finished))
                /* TODO: set icon for failed uploads */builder.setSmallIcon(R.drawable.frupic_notification_failed)
                builder.setContentText(getString(R.string.upload_notification_failed, max - failed, failed))
            }
            builder.setProgress(0, 0, false)
            builder.setAutoCancel(true)
            builder.setOngoing(false)
        }

        // TODO: set progress dialog intent when ongoing
        builder.setContentIntent(pendingIntent)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_UPLOAD = "upload"
    }
}