package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.GetReferenceIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String,
    @SerialName("current_status") val currentStatus: CurrentStatus,
    @Serializable(with = GetReferenceIdSerializer::class) @SerialName("stop") val stopId: String?
) {
    @Serializable
    enum class CurrentStatus {
        @SerialName("incoming_at") IncomingAt,
        @SerialName("stopped_at") StoppedAt,
        @SerialName("in_transit_to") InTransitTo
    }
}
