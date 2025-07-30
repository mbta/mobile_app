package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// The response doesn't include the route ID and direction ID, which we need in the UI to ensure
// that we're using the correct stop list for the selected route and direction.
@Serializable
data class RouteStopsResult(
    val routeId: String,
    val directionId: Int,
    val segments: List<RouteBranchSegment>,
)

interface IRouteStopsRepository {
    suspend fun getRouteSegments(routeId: String, directionId: Int): ApiResult<RouteStopsResult>
}

class RouteStopsRepository : IRouteStopsRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getRouteSegments(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResult> =
        ApiResult.runCatching {
            val response: List<RouteBranchSegment> =
                mobileBackendClient
                    .get {
                        url {
                            path("api/route/stop-graph")
                            parameters.append("route_id", routeId)
                            parameters.append("direction_id", directionId.toString())
                        }
                    }
                    .body()
            RouteStopsResult(routeId, directionId, response)
        }
}

class MockRouteStopsRepository(
    private val result: ApiResult<RouteStopsResult>,
    private val onGet: (String, Int) -> Unit = { _, _ -> },
) : IRouteStopsRepository {
    @DefaultArgumentInterop.Enabled
    constructor(
        segments: List<RouteBranchSegment>,
        routeId: String = "",
        directionId: Int = 0,
        onGet: (String, Int) -> Unit = { _, _ -> },
    ) : this(ApiResult.Ok(RouteStopsResult(routeId, directionId, segments)), onGet)

    override suspend fun getRouteSegments(
        routeId: String,
        directionId: Int,
    ): ApiResult<RouteStopsResult> {
        onGet(routeId, directionId)
        return result
    }
}
