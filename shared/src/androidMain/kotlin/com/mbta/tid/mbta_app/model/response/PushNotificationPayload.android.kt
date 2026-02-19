package com.mbta.tid.mbta_app.model.response

import androidx.work.Data
import androidx.work.workDataOf
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.RouteStopDirection
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

public fun PushNotificationPayload.Companion.messageToWorkData(
    messageData: Map<String, String>
): Data =
    workDataOf(
        "title" to messageData["title"],
        "summary" to messageData["summary"],
        "alert_id" to messageData["alert_id"],
        "subscriptions" to messageData["subscriptions"],
        "notification_type" to messageData["notification_type"],
        "sent_at" to messageData["sent_at"],
    )

public fun PushNotificationPayload.Companion.fromWorkData(
    workData: Data
): PushNotificationPayload? {
    val rawTitle = workData.getString("title") ?: return null
    val title: PushNotificationPayload.Title = json.decodeFromString(rawTitle)
    val rawSummary = workData.getString("summary") ?: return null
    val summary: AlertSummary = json.decodeFromString(rawSummary)
    val alertId = workData.getString("alert_id") ?: return null
    val rawSubscriptions = workData.getString("subscriptions") ?: return null
    val subscriptions: List<RouteStopDirection> = json.decodeFromString(rawSubscriptions)
    val rawNotificationType = workData.getString("notification_type") ?: return null
    val notificationType: PushNotificationPayload.NotificationType =
        json.decodeFromJsonElement(JsonPrimitive(rawNotificationType))
    val rawSentAt = workData.getString("sent_at") ?: return null
    val sentAt = Instant.parse(rawSentAt)
    return PushNotificationPayload(title, summary, alertId, subscriptions, notificationType, sentAt)
}
