package com.bharatconnect.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bharatconnect.app.MainActivity
import com.bharatconnect.app.R

object NotificationHelper {
    const val CHANNEL_MESSAGES_ID = "bharatconnect_messages"
    const val CHANNEL_MESSAGES_NAME = "Encrypted Messages"
    const val CHANNEL_SOCIAL_ID = "bharatconnect_social"
    const val CHANNEL_SOCIAL_NAME = "Feed & Community Updates"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct messages and group chat notifications"
                enableLights(true)
                enableVibration(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ID,
                CHANNEL_SOCIAL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Post likes, comments, and mentions"
            }

            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(socialChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        title: String,
        body: String,
        conversationId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
