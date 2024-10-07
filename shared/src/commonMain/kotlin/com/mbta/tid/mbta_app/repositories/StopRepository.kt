package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IStopRepository {
    suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse>
}

class StopRepository : IStopRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path("api/stop/map")
                        parameters.append("stop_id", stopId)
                    }
                }
                .body()
        }
}

class MockStopRepository : IStopRepository {
    override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> {
        return ApiResult.Ok(StopMapResponse(routeShapes = listOf(), childStops = mapOf()))
    }
}

class IdleStopRepository : IStopRepository {
    override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> {
        return suspendCancellableCoroutine {}
    }
}
