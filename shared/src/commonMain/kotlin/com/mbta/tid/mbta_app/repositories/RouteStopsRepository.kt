package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.RouteStopsResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// The response doesn't include the route ID and direction ID, which we need in the UI to ensure
// that we're using the correct stop list for the selected route and direction.
@Serializable
data class RouteStopsResult(val routeId: String, val directionId: Int, val stopIds: List<String>)

interface IRouteStopsRepository {
    suspend fun getRouteStops(routeId: String, directionId: Int): ApiResult<RouteStopsResult>
}

class RouteStopsRepository : IRouteStopsRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResult> =
        ApiResult.runCatching {
            val response: RouteStopsResponse =
                mobileBackendClient
                    .get {
                        url {
                            path("api/route/stops")
                            parameters.append("route_id", routeId)
                            parameters.append("direction_id", directionId.toString())
                        }
                    }
                    .body()
            RouteStopsResult(routeId, directionId, response.stopIds)
        }
}

class MockRouteStopsRepository(
    private val result: ApiResult<RouteStopsResult>,
    private val onGet: (String, Int) -> Unit = { _, _ -> },
) : IRouteStopsRepository {
    @DefaultArgumentInterop.Enabled
    constructor(
        stopIds: List<String>,
        routeId: String = "",
        directionId: Int = 0,
        onGet: (String, Int) -> Unit = { _, _ -> },
    ) : this(ApiResult.Ok(RouteStopsResult(routeId, directionId, stopIds)), onGet)

    override suspend fun getRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResult> {
        onGet(routeId, directionId)
        return result
    }
}
