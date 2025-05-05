package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import io.github.dellisd.spatialk.geojson.Position
import org.koin.core.component.KoinComponent

interface INearbyRepository {
    fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String>
}

class NearbyRepository : KoinComponent, INearbyRepository {
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

        return nearbyLeafStops
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
    }
}

class MockNearbyRepository(
    val response: NearbyResponse,
    val stopIds: List<String> = response.stopIds
) : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        stopIds
}

class IdleNearbyRepository : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        emptyList()
}
