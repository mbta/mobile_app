package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import io.github.dellisd.spatialk.geojson.Position
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent

interface INearbyRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData
}

class NearbyRepository : KoinComponent, INearbyRepository {
    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
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

        val nearbyStopsAndSiblings =
            nearbyLeafStops.flatMapTo(mutableSetOf()) { (stopId, distance) ->
                val stop = global.stops.getValue(stopId)
                val stopSiblings =
                    if (stop.parentStationId != null)
                        global.stops
                            .getValue(stop.parentStationId)
                            .childStopIds
                            .mapNotNull(global.stops::get)
                    else listOf(stop)
                val selectedStops =
                    if (distance <= radiusMiles) stopSiblings
                    else stopSiblings.filter { it.vehicleType == RouteType.COMMUTER_RAIL }
                selectedStops.map { it.id }
            }
        return NearbyStaticData(global, NearbyResponse(nearbyStopsAndSiblings.toList()))
    }
}

class MockNearbyRepository : INearbyRepository {
    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
        return NearbyStaticData(data = emptyList())
    }
}

class IdleNearbyRepository : INearbyRepository {
    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
        return suspendCancellableCoroutine {}
    }
}
