package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.cache.ResponseCache
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRailRouteShapeRepository {
    val state: StateFlow<MapFriendlyRouteResponse?>

    suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse>
}

class RailRouteShapeRepository(
    val cache: ResponseCache<MapFriendlyRouteResponse> =
        ResponseCache.create(cacheKey = "rail-route-shapes", invalidationKey = "2025-03-19")
) : IRailRouteShapeRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()
    override val state = cache.state

    override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> =
        cache.getOrFetch { etag: String? ->
            mobileBackendClient.get {
                timeout { requestTimeoutMillis = 10000 }
                url { path("api/shapes/map-friendly/rail") }
                header(HttpHeaders.IfNoneMatch, etag)
            }
        }
}

class MockRailRouteShapeRepository
@DefaultArgumentInterop.Enabled
constructor(
    val response: MapFriendlyRouteResponse = MapFriendlyRouteResponse(emptyList()),
    val onGet: () -> Unit = {}
) : IRailRouteShapeRepository {
    override val state = MutableStateFlow(response)

    override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> {
        onGet()
        return ApiResult.Ok(response)
    }
}

class IdleRailRouteShapeRepository : IRailRouteShapeRepository {
    override val state = MutableStateFlow(null)

    override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> {
        return suspendCancellableCoroutine {}
    }
}
