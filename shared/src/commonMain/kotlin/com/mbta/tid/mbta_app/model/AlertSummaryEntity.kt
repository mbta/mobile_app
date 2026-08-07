package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AlertSummaryEntity(
    @SerialName("alert_id") val alertId: String,
    @SerialName("route_id") val routeId: String?,
    @SerialName("stop_id") val stopId: String?,
    @SerialName("trip_id") val tripId: String?,
    @SerialName("direction_id") val directionId: Int?,
    val summary: String?,
)
