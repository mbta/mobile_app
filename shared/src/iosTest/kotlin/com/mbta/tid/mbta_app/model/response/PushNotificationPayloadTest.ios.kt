package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class PushNotificationPayloadTestIos {
    @Test
    fun `fromUserInfo works`() {
        val title: PushNotificationPayload.Title =
            PushNotificationPayload.Title.BareLabel("Red Line")
        val summary =
            AlertSummary(
                Alert.Effect.StationClosure,
                location = AlertSummary.Location.SingleStop("South Station"),
                timeframe = AlertSummary.Timeframe.Tomorrow,
            )
        val subscriptions = listOf(RouteStopDirection(Route.Id("route"), "stop", 0))
        val alertId = "alert"
        val sentAt = Clock.System.now()
        val userInfo =
            mapOf<Any, Any?>(
                "title" to json.encodeToString(title),
                "summary" to json.encodeToString(summary),
                "alert_id" to alertId,
                "subscriptions" to json.encodeToString(subscriptions),
                "notification_type" to "notification",
                "sent_at" to sentAt.toString(),
            )
        val payload = PushNotificationPayload.fromUserInfo(userInfo)
        assertEquals(
            PushNotificationPayload(
                title,
                summary,
                alertId,
                subscriptions,
                PushNotificationPayload.NotificationType.Notification,
                sentAt,
            ),
            payload,
        )
    }
}
