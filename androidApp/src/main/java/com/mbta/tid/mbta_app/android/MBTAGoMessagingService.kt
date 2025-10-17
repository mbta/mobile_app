package com.mbta.tid.mbta_app.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MBTAGoMessagingService : FirebaseMessagingService() {

    /**
     * If you want to target single devices or create device groups, you'll need this token. Because
     * the token could be rotated after initial startup, it is strongly recommended to retrieve the
     * latest updated registration token. If you need it any other time:
     * `FirebaseMessaging.getInstance().token.addOnCompleteListener {...}`
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // If the app is in the foreground we need to handle notifications coming in manually
        message.notification?.let {
            val title = it.title
            val body = it.body
            if (title != null && body != null) {
                sendNotification(title, body)
            }
        }
    }

    private fun sendNotification(title: String, body: String) {
        val requestCode = 0
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent =
            PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = ALERTS_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(
                channelId,
                getString(R.string.alerts_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManager.createNotificationChannel(channel)

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        const val ALERTS_CHANNEL_ID = "Alerts"
    }
}
