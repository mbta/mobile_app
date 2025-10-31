package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.distance
import org.maplibre.spatialk.units.Length

@Serializable
public data class Stop
internal constructor(
    override val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    @SerialName("location_type") val locationType: LocationType,
    val description: String? = null,
    @SerialName("platform_code") val platformCode: String? = null,
    @SerialName("platform_name") val platformName: String? = null,
    @SerialName("vehicle_type") val vehicleType: RouteType? = null,
    @SerialName("child_stop_ids") val childStopIds: List<String> = emptyList(),
    @SerialName("connecting_stop_ids") val connectingStopIds: List<String> = emptyList(),
    @SerialName("parent_station_id") val parentStationId: String? = null,
    @SerialName("wheelchair_boarding") val wheelchairBoarding: WheelchairBoardingStatus? = null,
) : BackendObject<String> {
    val position: Position = Position(latitude = latitude, longitude = longitude)

    /**
     * Commuter Rail core stations have realtime track numbers displayed and track change alerts
     * hidden.
     */
    val isCRCore: Boolean = this.id in crCoreStations || this.parentStationId in crCoreStations

    val shouldShowTrackNumber: Boolean =
        this.vehicleType == RouteType.COMMUTER_RAIL && this.isCRCore

    val isWheelchairAccessible: Boolean =
        wheelchairBoarding == WheelchairBoardingStatus.ACCESSIBLE ||
            this.vehicleType == RouteType.BUS

    internal fun resolveParent(stops: Map<String, Stop>): Stop {
        if (this.parentStationId == null) return this
        val parentStation = stops[parentStationId] ?: return this
        return parentStation.resolveParent(stops)
    }

    public fun resolveParent(global: GlobalResponse): Stop = resolveParent(global.stops)

    internal fun distanceFrom(position: Position): Length = distance(position, this.position)

    /**
     * Is this stop the last stop for all patterns in which it appears? True if for each patterns in
     * the given direction, the following conditions are met
     * - this stop (or its parent) only appears as the last stop
     * - this stop does not appear at all
     */
    public fun isLastStopForAllPatterns(
        directionId: Int,
        patterns: List<RoutePattern>,
        global: GlobalResponse,
    ): Boolean {
        val resolvedParent = this.resolveParent(global)
        return patterns
            .filter { it.directionId == directionId }
            .mapNotNull { global.trips[it.representativeTripId] }
            .mapNotNull {
                it.stopIds?.mapNotNull { stopId ->
                    global.getStop(stopId)?.resolveParent(global)?.id
                }
            }
            .all {
                val stopIndex = it.indexOf(resolvedParent.id)
                (stopIndex == it.lastIndex) || stopIndex == -1
            }
    }

    internal companion object {
        /**
         * Checks if the given stop IDs (as resolved in [stops]) refer to stops which are the same,
         * have the same parent, or are a parent and child.
         */
        fun equalOrFamily(stopId1: String, stopId2: String, stops: Map<String, Stop>): Boolean {
            if (stopId1 == stopId2) return true
            val stop1 = stops[stopId1] ?: return false
            val stop2 = stops[stopId2] ?: return false
            val parent1 = stop1.resolveParent(stops)
            val parent2 = stop2.resolveParent(stops)
            return parent1.id == parent2.id
        }

        val crCoreStations = setOf("place-north", "place-sstat", "place-bbsta", "place-rugg")

        /**
         * A map of a stop to itself and any children. for standalone stops, an entry will be
         * <standaloneStop, [standaloneStopId]>. for stations, an entry will be <station,
         * [stationId, child1Id, child2Id, etc.]>
         */
        fun resolvedParentToAllStops(
            stopIds: List<String>,
            globalData: GlobalResponse,
        ): Map<Stop, Set<String>> {
            return stopIds
                .mapNotNull { stopId ->
                    val stop = globalData.stops[stopId]
                    if (stop != null) {
                        Pair(stop.resolveParent(globalData), stop)
                    } else {
                        null
                    }
                }
                .groupBy({ it.first }, { it.second.id })
                .mapValues { it.value.toSet().plus(it.key.id) }
        }
    }
}
