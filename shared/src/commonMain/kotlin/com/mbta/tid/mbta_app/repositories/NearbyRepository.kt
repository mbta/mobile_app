package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import org.koin.core.component.KoinComponent
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Length
import org.maplibre.spatialk.units.extensions.miles

public interface INearbyRepository {
    /**
     * Gets the list of stops within 0.5 miles (or 1 mile for CR). Includes only stops with
     * [LocationType.STOP]. Omits stops that serves route pattern that are all served by closer
     * stops.
     */
    public fun getStopIdsNearby(global: GlobalResponse, location: Position): NearbyResponse
}

public class NearbyRepository : KoinComponent, INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): NearbyResponse {
        val searchPosition = Position(latitude = location.latitude, longitude = location.longitude)

        var radius = 0.5.miles
        var crRadius = 1.0.miles

        fun findLeafStops(): List<Pair<String, Length>> =
            global.leafStopsKdTree.findNodesWithin(searchPosition, crRadius) { stopId, distance ->
                distance < radius || global.stops[stopId]?.vehicleType == RouteType.COMMUTER_RAIL
            }

        var nearbyLeafStops = findLeafStops()

        if (nearbyLeafStops.isEmpty()) {
            radius = 2.0.miles
            crRadius = 10.0.miles
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
                        if (distance <= radius) stopSiblings
                        else stopSiblings.filter { it.vehicleType == RouteType.COMMUTER_RAIL }
                    selectedStops.map { it.id }
                }
                .filterNot {
                    global.getStop(it)?.resolveParent(global)?.id?.let { stopId ->
                        global.stopBlocklist.contains(stopId)
                    } ?: false
                }
                .toList()

        return NearbyResponse(allNearbyStops)
    }
}

public class MockNearbyRepository(private val response: NearbyResponse) : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): NearbyResponse =
        response
}

internal class IdleNearbyRepository : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): NearbyResponse =
        NearbyResponse(emptyList())
}
