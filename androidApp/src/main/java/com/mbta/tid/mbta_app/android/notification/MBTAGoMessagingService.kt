package com.mbta.tid.mbta_app.android.notification

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mbta.tid.mbta_app.android.util.fcmToken

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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val workRequest =
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(workDataOf("summary" to message.data["summary"]))
                .build()
        WorkManager.Companion.getInstance(this).enqueue(workRequest)
    }
}
