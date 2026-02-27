package com.mbta.tid.mbta_app.model.response

import androidx.work.workDataOf
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class PushNotificationPayloadTestAndroid {
    @Test
    fun `messageToWorkData works`() {
        val title =
            json.encodeToString(
                buildJsonObject {
                    put("type", "bare_label")
                    put("label", "Red Line")
                }
            )
        val summary =
            json.encodeToString(
                buildJsonObject {
                    put("effect", "station_closure")
                    putJsonObject("location") {
                        put("type", "single_stop")
                        put("stop_name", "South Station")
                    }
                    putJsonObject("timeframe") { put("type", "tomorrow") }
                }
            )
        val subscriptions =
            json.encodeToString(
                buildJsonArray {
                    addJsonObject {
                        put("route", "1")
                        put("stop", "1")
                        put("direction", 1)
                    }
                }
            )
        val messageData =
            mapOf(
                Pair("title", title),
                Pair("summary", summary),
                Pair("alert_id", "alert"),
                Pair("subscriptions", subscriptions),
                Pair("notification_type", "notification"),
                Pair("sent_at", "2026-02-17T14:57:00-07:00"),
            )
        val workData = PushNotificationPayload.messageToWorkData(messageData)
        assertEquals(
            workDataOf(
                "title" to title,
                "summary" to summary,
                "alert_id" to "alert",
                "subscriptions" to subscriptions,
                "notification_type" to "notification",
                "sent_at" to "2026-02-17T14:57:00-07:00",
            ),
            workData,
        )
    }

    @Test
    fun `fromWorkData works`() {
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
        val workData =
            workDataOf(
                "title" to json.encodeToString(title),
                "summary" to json.encodeToString(summary),
                "alert_id" to alertId,
                "subscriptions" to json.encodeToString(subscriptions),
                "notification_type" to "notification",
                "sent_at" to sentAt.toString(),
            )
        val payload = PushNotificationPayload.fromWorkData(workData)
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
