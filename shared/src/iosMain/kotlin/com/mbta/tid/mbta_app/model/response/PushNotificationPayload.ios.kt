package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.RouteStopDirection
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

public fun PushNotificationPayload.Companion.fromUserInfo(
    userInfo: Map<Any, Any?>
): PushNotificationPayload? {
    val rawTitle = userInfo["title"] as? String ?: return null
    val title: PushNotificationPayload.Title = json.decodeFromString(rawTitle)
    val rawSummary = userInfo["summary"] as? String ?: return null
    val summary: AlertSummary = json.decodeFromString(rawSummary)
    val alertId = userInfo["alert_id"] as? String ?: return null
    val rawSubscriptions = userInfo["subscriptions"] as? String ?: return null
    val subscriptions: List<RouteStopDirection> = json.decodeFromString(rawSubscriptions)
    val rawNotificationType = userInfo["notification_type"] as? String ?: return null
    val notificationType: PushNotificationPayload.NotificationType =
        json.decodeFromJsonElement(JsonPrimitive(rawNotificationType))
    val rawSentAt = userInfo["sent_at"] as? String ?: return null
    val sentAt = Instant.parse(rawSentAt)
    return PushNotificationPayload(title, summary, alertId, subscriptions, notificationType, sentAt)
}
