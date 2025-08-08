package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface IStopRepository {
    public suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse>
}

internal class StopRepository : IStopRepository, KoinComponent {

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

public class MockStopRepository(private val objects: ObjectCollectionBuilder? = null) :
    IStopRepository {
    override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> {

        if (objects == null) {
            ApiResult.Ok(StopMapResponse(listOf(), mapOf()))
        }

        val asGlobal = GlobalResponse(objects!!)

        return ApiResult.Ok(
            StopMapResponse(
                asGlobal.getPatternsFor(stopId).map {
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        it.routeId,
                        listOf(
                            SegmentedRouteShape(
                                it.id,
                                it.routeId,
                                it.directionId,
                                listOf(),
                                Shape("fake-shape${it.id}"),
                            )
                        ),
                    )
                },
                childStops =
                    objects.getStop(stopId).childStopIds.associateWith { childId ->
                        objects.getStop(childId)
                    },
            )
        )
    }
}

internal class IdleStopRepository : IStopRepository {
    override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> {
        return suspendCancellableCoroutine {}
    }
}
