package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.RouteStopsResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRouteStopsRepository {
    suspend fun getRouteStops(routeId: String, directionId: Int): ApiResult<RouteStopsResponse>
}

class RouteStopsRepository : IRouteStopsRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path("api/route/stops")
                        parameters.append("route_id", routeId)
                        parameters.append("direction_id", directionId.toString())
                    }
                }
                .body()
        }
}

class MockRouteStopsRepository(
    private val result: ApiResult<RouteStopsResponse>,
    private val onGet: (String, Int) -> Unit = { _, _ -> },
) : IRouteStopsRepository {
    @DefaultArgumentInterop.Enabled
    constructor(
        stopIds: List<String>,
        onGet: (String, Int) -> Unit = { _, _ -> },
    ) : this(ApiResult.Ok(RouteStopsResponse(stopIds)), onGet)

    override suspend fun getRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResponse> {
        onGet(routeId, directionId)
        return result
    }
}
