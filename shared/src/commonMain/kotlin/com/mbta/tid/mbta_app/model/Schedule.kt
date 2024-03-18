package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    override val id: String,
    @SerialName("arrival_time") val arrivalTime: Instant?,
    @SerialName("departure_time") val departureTime: Instant?,
    @SerialName("drop_off_type") val dropOffType: StopEdgeType,
    @SerialName("pick_up_type") val pickUpType: StopEdgeType,
    @SerialName("stop_sequence") val stopSequence: Int,
    @SerialName("route_id") val routeId: String,
    @SerialName("stop_id") val stopId: String,
    @SerialName("trip_id") val tripId: String,
) : Comparable<Schedule>, BackendObject {
    val scheduleTime = arrivalTime ?: departureTime

    override fun compareTo(other: Schedule): Int =
        nullsLast<Instant>().compare(scheduleTime, other.scheduleTime)

    @Serializable
    enum class StopEdgeType {
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
