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
    suspend fun getNearby(global: GlobalResponse, location: Position): ApiResult<NearbyStaticData>

    suspend fun getNearbyStops(global: GlobalResponse, location: Position): ApiResult<Set<String>>
}

class NearbyRepository : KoinComponent, INearbyRepository {
    override suspend fun getNearby(
        global: GlobalResponse,
        location: Position
    ): ApiResult<NearbyStaticData> {

        return when (val nearbyStopsResult = getNearbyStops(global, location)) {
            is ApiResult.Error -> ApiResult.Error(nearbyStopsResult.code, nearbyStopsResult.message)
            is ApiResult.Ok ->
                ApiResult.Ok(
                    NearbyStaticData(global, NearbyResponse(nearbyStopsResult.data.toList()))
                )
        }
    }

    override suspend fun getNearbyStops(
        global: GlobalResponse,
        location: Position
    ): ApiResult<Set<String>> {
        return ApiResult.runCatching {
            val searchPosition =
                Position(latitude = location.latitude, longitude = location.longitude)

            var radiusMiles = 0.5
            var crRadiusMiles = 1.0

            fun findLeafStops(): List<Pair<String, Double>> =
                global.leafStopsKdTree.findNodesWithin(searchPosition, crRadiusMiles) {
                    stopId,
                    distance ->
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

            nearbyStopsAndSiblings
        }
    }
}

class MockNearbyRepository(val result: NearbyStaticData = NearbyStaticData(data = emptyList())) :
    INearbyRepository {
    override suspend fun getNearby(
        global: GlobalResponse,
        location: Position
    ): ApiResult<NearbyStaticData> {
        return ApiResult.Ok(result)
    }

    override suspend fun getNearbyStops(
        global: GlobalResponse,
        location: Position
    ): ApiResult<Set<String>> {
        return ApiResult.Ok(result.stopIds())
    }
}

class IdleNearbyRepository : INearbyRepository {
    override suspend fun getNearby(
        global: GlobalResponse,
        location: Position
    ): ApiResult<NearbyStaticData> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getNearbyStops(
        global: GlobalResponse,
        location: Position
    ): ApiResult<Set<String>> {
        return suspendCancellableCoroutine {}
    }
}
