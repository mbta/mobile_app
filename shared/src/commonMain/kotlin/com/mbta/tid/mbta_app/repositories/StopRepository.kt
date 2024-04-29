package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IStopRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getStopMapData(stopId: String): StopMapResponse
}

class StopRepository : IStopRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getStopMapData(stopId: String): StopMapResponse =
        mobileBackendClient
            .get {
                url {
                    path("api/stop/map")
                    parameters.append("stop_id", stopId)
                }
            }
            .body()
}

class MockStopRepository : IStopRepository {
    override suspend fun getStopMapData(stopId: String): StopMapResponse {
        return StopMapResponse(routeShapes = listOf(), stops = mapOf())
    }
}

class IdleStopRepository : IStopRepository {
    override suspend fun getStopMapData(stopId: String): StopMapResponse {
        return suspendCancellableCoroutine {}
    }
}
