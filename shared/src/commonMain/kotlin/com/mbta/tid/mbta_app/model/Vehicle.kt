package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    override val id: String,
    @SerialName("current_status") val currentStatus: CurrentStatus,
    @SerialName("stop_id") val stopId: String?,
    @SerialName("trip_id") val tripId: String?,
) : BackendObject {
    @Serializable
    enum class CurrentStatus {
        @SerialName("incoming_at") IncomingAt,
        @SerialName("stopped_at") StoppedAt,
        @SerialName("in_transit_to") InTransitTo
    }
}
