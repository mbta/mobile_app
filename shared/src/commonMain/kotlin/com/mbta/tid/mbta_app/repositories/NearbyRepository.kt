package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import io.github.dellisd.spatialk.geojson.Position
import org.koin.core.component.KoinComponent

public interface INearbyRepository {
    /**
     * Gets the list of stops within 0.5 miles (or 1 mile for CR). Includes only stops with
     * [LocationType.STOP]. Omits stops that serves route pattern that are all served by closer
     * stops.
     */
    public fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String>
}

public class NearbyRepository : KoinComponent, INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> {
        val searchPosition = Position(latitude = location.latitude, longitude = location.longitude)

        var radiusMiles = 0.5
        var crRadiusMiles = 1.0

        fun findLeafStops(): List<Pair<String, Double>> =
            global.leafStopsKdTree.findNodesWithin(searchPosition, crRadiusMiles) { stopId, distance
                ->
                distance < radiusMiles ||
                    global.stops[stopId]?.vehicleType == RouteType.COMMUTER_RAIL
            }

        var nearbyLeafStops = findLeafStops()

        if (nearbyLeafStops.isEmpty()) {
            radiusMiles = 2.0
            crRadiusMiles = 10.0
            nearbyLeafStops = findLeafStops()
        }

        val allNearbyStops =
            nearbyLeafStops
                .flatMapTo(mutableSetOf()) { (stopId, distance) ->
                    val stop = global.stops[stopId] ?: return@flatMapTo emptyList()
                    val stopSiblings =
                        if (stop.parentStationId != null)
                            global.stops[stop.parentStationId]
                                ?.childStopIds
                                .orEmpty()
                                .mapNotNull(global.stops::get)
                        else listOf(stop)
                    val selectedStops =
                        if (distance <= radiusMiles) stopSiblings
                        else stopSiblings.filter { it.vehicleType == RouteType.COMMUTER_RAIL }
                    selectedStops.map { it.id }
                }
                .toList()

        return filterStopsWithRedundantPatterns(allNearbyStops, global)
    }

    /**
     * Filter the given list of stopIds to the stops that don't have service redundant to earlier
     * stops in the list; each stop must serve at least one route pattern that is not seen by any
     * earlier stop.
     */
    private fun filterStopsWithRedundantPatterns(
        stopIds: List<String>,
        globalData: GlobalResponse,
    ): List<String> {
        val originalStopIdSet = stopIds.toSet()
        val originalStopOrder = stopIds.mapIndexed { index, id -> Pair(id, index) }.toMap()

        val parentToAllStops = Stop.resolvedParentToAllStops(stopIds, globalData)

        return RoutePattern.patternsGroupedByLineOrRouteAndStop(
                parentToAllStops,
                globalData,
                context = RouteCardData.Context.NearbyTransit,
            )
            .flatMap {
                it.value.keys.flatMap { stop ->
                    stop.childStopIds.toSet().plus(stop.id).intersect(originalStopIdSet)
                }
            }
            .distinct()
            .sortedBy { originalStopOrder[it] }
    }
}

public class MockNearbyRepository(
    private val response: NearbyResponse,
    private val stopIds: List<String> = response.stopIds,
) : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        stopIds
}

internal class IdleNearbyRepository : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        emptyList()
}
