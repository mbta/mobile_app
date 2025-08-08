package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Schedule
internal constructor(
    override val id: String,
    @SerialName("arrival_time") override val arrivalTime: EasternTimeInstant?,
    @SerialName("departure_time") override val departureTime: EasternTimeInstant?,
    @SerialName("drop_off_type") internal val dropOffType: StopEdgeType,
    @SerialName("pick_up_type") internal val pickUpType: StopEdgeType,
    @SerialName("stop_headsign") val stopHeadsign: String?,
    @SerialName("stop_sequence") internal val stopSequence: Int,
    @SerialName("route_id") internal val routeId: String,
    @SerialName("stop_id") internal val stopId: String,
    @SerialName("trip_id") internal val tripId: String,
) : BackendObject, TripStopTime {
    @Serializable
    internal enum class StopEdgeType {
        /** Regularly scheduled drop off / pickup */
        @SerialName("regular") Regular,
        /** No drop off / pickup available */
        @SerialName("unavailable") Unavailable,
        /** Must phone agency to arrange drop off / pickup */
        @SerialName("call_agency") CallAgency,
        /** Must coordinate with driver to arrange drop off / pickup */
        @SerialName("coordinate_with_driver") CoordinateWithDriver,
    }
}
