package com.mbta.tid.mbta_app.android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mbta.tid.mbta_app.android.MainActivity
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fcmToken
import kotlin.random.Random

class MBTAGoMessagingService : FirebaseMessagingService() {

    /**
     * If you want to target single devices or create device groups, you'll need this token. Because
     * the token could be rotated after initial startup, it is strongly recommended to retrieve the
     * latest updated registration token. If you need it any other time:
     * `FirebaseMessaging.getInstance().token.addOnCompleteListener {...}`
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmToken = token
    }

    // if a notification is sent when the app is in the foreground, we have to create the
    // notification ourselves, for some reason
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        for (messageDatum in message.data) {
            intent.putExtra(messageDatum.key, messageDatum.value)
        }
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                Random.nextInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val channelId = getString(R.string.alerts_channel)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.app_icon_monochrome)
                .setContentTitle(message.notification?.title)
                .setContentText(message.notification?.body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val channel =
            NotificationChannel(
                channelId,
                applicationContext.getString(R.string.alerts_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        channel.setSound(
            defaultSoundUri,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build(),
        )

        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}
