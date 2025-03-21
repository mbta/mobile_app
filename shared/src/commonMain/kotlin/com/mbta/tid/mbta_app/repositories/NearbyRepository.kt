package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent

interface INearbyRepository {
    fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String>

    suspend fun getNearby(global: GlobalResponse, location: Position): ApiResult<NearbyStaticData>
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

    override suspend fun getNearby(global: GlobalResponse, location: Position) =
        ApiResult.runCatching {
            NearbyStaticData(global, NearbyResponse(getStopIdsNearby(global, location)))
        }
}

class MockNearbyRepository(val response: NearbyResponse, val stopIds: List<String> = emptyList()) :
    INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        stopIds

    override suspend fun getNearby(
        global: GlobalResponse,
        location: Position
    ): ApiResult<NearbyStaticData> {
        return ApiResult.Ok(NearbyStaticData(global, response))
    }
}

class IdleNearbyRepository : INearbyRepository {
    override fun getStopIdsNearby(global: GlobalResponse, location: Position): List<String> =
        emptyList()

    override suspend fun getNearby(
        global: GlobalResponse,
        location: Position
    ): ApiResult<NearbyStaticData> {
        return suspendCancellableCoroutine {}
    }
}
