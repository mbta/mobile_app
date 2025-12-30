package com.mbta.tid.mbta_app.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mbta.tid.mbta_app.android.MainActivity
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    private val alertsRepository: IAlertsRepository by inject()
    private val globalRepository: IGlobalRepository by inject()

    override suspend fun doWork(): Result {
        val alertId = inputData.getString("alertId") ?: return Result.failure()
        val rawSubscriptions = inputData.getString("subscriptions") ?: return Result.failure()
        val subscriptions: List<RouteStopDirection> = json.decodeFromString(rawSubscriptions)
        val requestCode = 0
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val content =
            NotificationContent.build(
                applicationContext.resources,
                alertId,
                subscriptions,
                alertsRepository,
                globalRepository,
            ) ?: return Result.failure()
        val title = content.title
        val body = content.body

        val channelId = ALERTS_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
        return Result.success()
    }

    companion object {
        const val ALERTS_CHANNEL_ID = "Alerts"
    }
}
