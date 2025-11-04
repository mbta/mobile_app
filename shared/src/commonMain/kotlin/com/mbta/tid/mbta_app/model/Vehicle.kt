package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.maplibre.spatialk.geojson.Position

@Serializable
public data class Vehicle(
    override val id: String,
    val bearing: Double?,
    @SerialName("current_status") val currentStatus: CurrentStatus,
    @SerialName("current_stop_sequence") val currentStopSequence: Int?,
    @SerialName("direction_id") val directionId: Int,
    val latitude: Double,
    val longitude: Double,
    @SerialName("updated_at") val updatedAt: EasternTimeInstant,
    @SerialName("route_id") val routeId: Route.Id?,
    @SerialName("stop_id") val stopId: String?,
    @SerialName("trip_id") val tripId: String?,
) : BackendObject<String> {
    val position: Position = Position(latitude = latitude, longitude = longitude)

    @Serializable
    public enum class CurrentStatus {
        @SerialName("incoming_at") IncomingAt,
        @SerialName("stopped_at") StoppedAt,
        @SerialName("in_transit_to") InTransitTo,
    }

    override fun toString(): String = "Vehicle(id=$id)"
}
