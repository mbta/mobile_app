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
    public fun isScheduledTrackChange(trip: Trip, globalData: GlobalResponse): Boolean {
        if (globalData.routes[routeId]?.type != RouteType.COMMUTER_RAIL) return false

        val stop = globalData.getStop(stopId) ?: return false

        val canonicalTrip =
            globalData
                .getPatternsFor(this.routeId, trip.directionId)
                .firstOrNull { it.canonical }
                ?.representativeTripId ?: return false
        val canonicalStopIds = globalData.trips[canonicalTrip]?.stopIds?.toSet() ?: return false

        if (canonicalStopIds.contains(stopId)) return false

        // If there is no canonical platformCode for this stop, then it is common for the
        // track to be different between trips, and we don't need to treat it as a track change.
        return !canonicalStopIds.any {
            globalData.getStop(it)?.let { canonicalStop ->
                stop.parentStationId != null &&
                    canonicalStop.parentStationId == stop.parentStationId &&
                    canonicalStop.platformCode == null
            } ?: false
        }
    }
}
