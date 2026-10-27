package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Schedule
internal constructor(
    override val id: String,
    @SerialName("arrival_time") override val arrivalTime: EasternTimeInstant?,
    @SerialName("departure_time") override val departureTime: EasternTimeInstant?,
    @SerialName("drop_off_type") val dropOffType: StopEdgeType,
    @SerialName("pick_up_type") val pickUpType: StopEdgeType,
    @SerialName("stop_headsign") val stopHeadsign: String?,
    @SerialName("stop_sequence") val stopSequence: Int,
    @SerialName("route_id") val routeId: Route.Id,
    @SerialName("stop_id") val stopId: String,
    @SerialName("trip_id") val tripId: String,
) : BackendObject<String>, TripStopTime {
    @Serializable
    public enum class StopEdgeType {
        /** Regularly scheduled drop off / pickup */
        @SerialName("regular") Regular,
        /** No drop off / pickup available */
        @SerialName("unavailable") Unavailable,
        /** Must phone agency to arrange drop off / pickup */
        @SerialName("call_agency") CallAgency,
        /** Must coordinate with driver to arrange drop off / pickup */
        @SerialName("coordinate_with_driver") CoordinateWithDriver,
    }

    /**
     * Is the scheduled stop id different from the canonically scheduled stop id. For commuter rail
     * trips only.
     */
    public fun isScheduledTrackChange(globalData: GlobalResponse): Boolean {
        val trip = globalData.trips[tripId] ?: return false
        if (globalData.routes[routeId]?.type != (RouteType.COMMUTER_RAIL)) {
            return false
        }
        val canonicalStopIds =
            globalData
                .getPatternsFor(this.routeId, trip.directionId)
                .filter { it.canonical }
                .flatMap {
                    globalData.trips[it.representativeTripId]?.stopIds?.toSet() ?: emptySet()
                }

        if (canonicalStopIds.isEmpty()) {
            return false
        }

        return !canonicalStopIds.contains(stopId)
    }
}
