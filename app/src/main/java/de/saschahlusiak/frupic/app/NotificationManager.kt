package de.saschahlusiak.frupic.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.Reusable
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.grid.GridActivity
import javax.inject.Inject

@Reusable
class NotificationManager @Inject constructor(
    private val context: Context
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

    fun updateUnseenNotification(newFrupics: Int) {
        if (newFrupics == 0) {
            clearUnseenNotification()
            return
        }

        val intent = Intent(context, GridActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_UNSEEN).apply {
            color = context.getColor(R.color.brand_yellow)
            setContentTitle(context.getString(R.string.refresh_service_count_text, newFrupics))
            setContentText(context.getString(R.string.refresh_service_title))
            setSmallIcon(R.drawable.frupic_notification)
            setNumber(newFrupics)
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