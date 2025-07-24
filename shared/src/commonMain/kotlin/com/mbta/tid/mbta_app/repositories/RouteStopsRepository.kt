package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteBranchSegment
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
data class OldRouteStopsResult(
    val routeId: String,
    val directionId: Int,
    val stopIds: List<String>,
)

@Serializable
data class NewRouteStopsResult(
    val routeId: String,
    val directionId: Int,
    val segments: List<RouteBranchSegment>,
)

interface IRouteStopsRepository {
    suspend fun getOldRouteStops(routeId: String, directionId: Int): ApiResult<OldRouteStopsResult>

    suspend fun getNewRouteSegments(
        routeId: String,
        directionId: Int,
    ): ApiResult<NewRouteStopsResult>
}

class RouteStopsRepository : IRouteStopsRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getOldRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<OldRouteStopsResult> =
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
            OldRouteStopsResult(routeId, directionId, response.stopIds)
        }

    override suspend fun getNewRouteSegments(
        routeId: String,
        directionId: Int,
    ): ApiResult<NewRouteStopsResult> =
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
            NewRouteStopsResult(routeId, directionId, response)
        }
}

class MockRouteStopsRepository(
    private val oldResult: ApiResult<OldRouteStopsResult>?,
    private val newResult: ApiResult<NewRouteStopsResult>?,
    private val onGet: (String, Int) -> Unit = { _, _ -> },
) : IRouteStopsRepository {
    @DefaultArgumentInterop.Enabled
    constructor(
        stopIds: List<String>? = null,
        segments: List<RouteBranchSegment>? = null,
        routeId: String = "",
        directionId: Int = 0,
        onGet: (String, Int) -> Unit = { _, _ -> },
    ) : this(
        stopIds?.let { ApiResult.Ok(OldRouteStopsResult(routeId, directionId, it)) },
        segments?.let { ApiResult.Ok(NewRouteStopsResult(routeId, directionId, it)) },
        onGet,
    )

    override suspend fun getOldRouteStops(
        routeId: String,
        directionId: Int,
    ): ApiResult<OldRouteStopsResult> {
        onGet(routeId, directionId)
        return checkNotNull(oldResult) { "old result not defined" }
    }

    override suspend fun getNewRouteSegments(
        routeId: String,
        directionId: Int,
    ): ApiResult<NewRouteStopsResult> {
        onGet(routeId, directionId)
        return checkNotNull(newResult) { "new result not defined" }
    }
}
