package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.Route
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
public data class RouteStopsResult(
    internal val routeId: Route.Id,
    internal val directionId: Int,
    internal val segments: List<RouteBranchSegment>,
)

public interface IRouteStopsRepository {
    public suspend fun getRouteSegments(
        routeId: Route.Id,
        directionId: Int,
    ): ApiResult<RouteStopsResult>
}

internal class RouteStopsRepository : IRouteStopsRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getRouteSegments(
        routeId: Route.Id,
        directionId: Int,
    ): ApiResult<RouteStopsResult> =
        ApiResult.runCatching {
            val response: List<RouteBranchSegment> =
                mobileBackendClient
                    .get {
                        url {
                            path("api/route/stop-graph")
                            parameters.append("route_id", routeId.idText)
                            parameters.append("direction_id", directionId.toString())
                        }
                    }
                    .body()
            RouteStopsResult(routeId, directionId, response)
        }
}

public class MockRouteStopsRepository(
    private val result: ApiResult<RouteStopsResult>,
    private val onGet: (Route.Id, Int) -> Unit = { _, _ -> },
) : IRouteStopsRepository {
    @DefaultArgumentInterop.Enabled
    public constructor(
        segments: List<RouteBranchSegment>,
        routeId: Route.Id = Route.Id(""),
        directionId: Int = 0,
        onGet: (Route.Id, Int) -> Unit = { _, _ -> },
    ) : this(ApiResult.Ok(RouteStopsResult(routeId, directionId, segments)), onGet)

    override suspend fun getRouteSegments(
        routeId: Route.Id,
        directionId: Int,
    ): ApiResult<RouteStopsResult> {
        onGet(routeId, directionId)
        return result
    }
}
