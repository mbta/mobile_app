package com.mbta.tid.mbta_app.android.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.service.notification.StatusBarNotification
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

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.app_icon_monochrome)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // A message having alert_id in the body means it is a data only notification to help
        // Android notifications group together. It will update the ungrouped notification with the
        // same tag
        if (message.data.containsKey("alert_id")) {
            val alertId = message.data["alert_id"]
            val ungroupedNotification =
                findUngroupedNotification(notificationManager, message.data["tag"]) ?: return

            updateNotification(
                notificationManager,
                notificationBuilder,
                ungroupedNotification,
                alertId,
                channelId,
                message,
            )
        } else {
            notificationBuilder
                .setContentTitle(message.notification?.title)
                .setContentText(message.notification?.body)
            notificationManager.notify(
                message.notification?.tag,
                0,
                notificationBuilder.build(),
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(
        notificationManager: NotificationManagerCompat,
        notificationBuilder: NotificationCompat.Builder,
        ungroupedNotification: StatusBarNotification,
        alertId: String?,
        channelId: String,
        message: RemoteMessage,
    ) {
        // Try using the title and body from the previous notification, if they exist.
        // Otherwise, use the title and body from the new message.
        val title =
            ungroupedNotification.notification.extras.getString(Notification.EXTRA_TITLE)
                ?: message.data.getOrDefault("title", null)
        val body =
            ungroupedNotification.notification.extras.getString(Notification.EXTRA_TEXT)
                ?: message.data.getOrDefault("body", null)
        notificationBuilder
            .setContentTitle(title)
            .setContentText(body)
            .setOnlyAlertOnce(true)
            .setGroup(alertId)

        notificationManager.notify(
            ungroupedNotification.tag,
            ungroupedNotification.id,
            notificationBuilder.build(),
        )

        // A notification summary is required, using latest notification title and body.
        val summaryNotification =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.app_icon_monochrome)
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(alertId)
                .setGroupSummary(true)
                .build()

        notificationManager.notify(
            "summary-${alertId}",
            ungroupedNotification.id,
            summaryNotification,
        )
    }

    private fun findUngroupedNotification(
        notificationManager: NotificationManagerCompat,
        tag: String?,
    ): StatusBarNotification? {
        val activeNotifications = notificationManager.activeNotifications
        // Finding notification by filtering out all the ones that already have a group
        // and then checking if the tag matches the one we are looking for
        return activeNotifications.find { notification ->
            !notification.isGroup && notification.tag != null && notification.tag == tag
        }
    }
}
