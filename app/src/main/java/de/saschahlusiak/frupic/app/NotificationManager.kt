package de.saschahlusiak.frupic.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.grid.GridActivity
import de.saschahlusiak.frupic.model.Frupic
import javax.inject.Inject

class NotificationManager @Inject constructor(
    private val context: Context,
    private val repository: FrupicRepository
) {
    private val nm = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_UNSEEN, context.getString(R.string.channel_name_unseen), NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(channel)
    }

    suspend fun updateUnseenNotification() {
        // This are all FruPics that are "new", i.e. have been fetched after the user has last opened the app
        // They are cleared when the app is launched or finished
        val new = repository.getFrupicCount(Frupic.FLAG_NEW)

        if (new == 0) {
            clearUnseenNotification()
            return
        }

        // These are the FruPics the user has not been notified about
        // I.e. FruPics remain NEW but once a notification has been shown they are SEEN
        // We clear this flag once a notification is shown, so notifications are not repeated
        val unseen = repository.getFrupicCount(Frupic.FLAG_NEW or Frupic.FLAG_NEED_NOTIFICATION)
        Log.d(tag, "Updating notification for $new unseen frupics")

        if (unseen == 0) {
            // We have already notified about all new frupics
            return
        }

        // Clearing this flag means we will not trigger notifications again for this frupic (but still include it in the count)
        repository.removeFlags(Frupic.FLAG_NEED_NOTIFICATION)

        val intent = Intent(context, GridActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_UNSEEN).apply {
            color = context.getColor(R.color.brand_yellow_bright)
            setContentTitle(context.getString(R.string.refresh_service_count_text, new))
            setContentText(context.getString(R.string.refresh_service_title))
            setSmallIcon(R.drawable.frupic_notification_new)
            setNumber(new)
            setAutoCancel(true)
            setOngoing(false)

            setContentIntent(pendingIntent)
        }

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    fun clearUnseenNotification() {
        nm.cancel(NOTIFICATION_ID)
    }

    companion object {
        private val tag = NotificationManager::class.simpleName
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_UNSEEN = "unseen"
    }
}