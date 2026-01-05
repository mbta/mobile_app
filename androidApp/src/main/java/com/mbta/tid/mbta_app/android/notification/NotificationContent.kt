package com.mbta.tid.mbta_app.android.notification

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.model.AlertSummary

data class NotificationContent(val title: AnnotatedString, val body: AnnotatedString) {
    companion object {
        fun build(resources: Resources, alertSummary: AlertSummary): NotificationContent {
            val formattedAlert = FormattedAlert(alert = null, alertSummary)

            val title = formattedAlert.alertCardHeader(AlertCardSpec.Major, resources)
            val body = formattedAlert.alertCardMajorBody(resources)

            return NotificationContent(title, body)
        }
    }
}
