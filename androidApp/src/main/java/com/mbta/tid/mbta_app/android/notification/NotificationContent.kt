package com.mbta.tid.mbta_app.android.notification

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.response.PushNotificationPayload

data class NotificationContent(val title: String, val body: AnnotatedString) {
    companion object {
        private fun getTitle(resources: Resources, title: PushNotificationPayload.Title): String =
            when (title) {
                is PushNotificationPayload.Title.BareLabel -> title.label
                is PushNotificationPayload.Title.ModeLabel ->
                    resources.getString(
                        R.string.route_mode_label,
                        title.label,
                        title.mode.typeText(resources, isOnly = true),
                    )
                PushNotificationPayload.Title.MultipleRoutes ->
                    resources.getString(R.string.multiple_routes)
                is PushNotificationPayload.Title.Unknown -> getTitle(resources, title.fallback)
            }

        fun build(resources: Resources, payload: PushNotificationPayload): NotificationContent {
            val formattedAlert = FormattedAlert(alert = null, payload.summary)

            val title = getTitle(resources, payload.title)
            val body = formattedAlert.alertCardMajorBody(resources)

            return NotificationContent(title, body)
        }
    }
}
