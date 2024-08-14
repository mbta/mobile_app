package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRailRouteShapeRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getRailRouteShapes(): MapFriendlyRouteResponse
}

class RailRouteShapeRepository : IRailRouteShapeRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getRailRouteShapes(): MapFriendlyRouteResponse =
        mobileBackendClient
            .get {
                url { path("api/shapes/map-friendly/rail") }
                expectSuccess = true
            }
            .body()
}

class MockRailRouteShapeRepository
@DefaultArgumentInterop.Enabled
constructor(
    val response: MapFriendlyRouteResponse = MapFriendlyRouteResponse(emptyList()),
    val onGet: () -> Unit = {}
) : IRailRouteShapeRepository {
    override suspend fun getRailRouteShapes(): MapFriendlyRouteResponse {
        onGet()
        return response
    }
}

class IdleRailRouteShapeRepository : IRailRouteShapeRepository {
    override suspend fun getRailRouteShapes(): MapFriendlyRouteResponse {
        return suspendCancellableCoroutine {}
    }
}
