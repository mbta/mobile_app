package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.cache.ResponseCache
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
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

public interface IRailRouteShapeRepository {
    public val state: StateFlow<MapFriendlyRouteResponse?>

    public suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse>
}

internal class RailRouteShapeRepository(
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

public class MockRailRouteShapeRepository
@DefaultArgumentInterop.Enabled
constructor(
    private val response: MapFriendlyRouteResponse = MapFriendlyRouteResponse(emptyList()),
    private val onGet: () -> Unit = {},
) : IRailRouteShapeRepository {

    public constructor(
        objects: ObjectCollectionBuilder
    ) : this(
        response =
            MapFriendlyRouteResponse(
                objects.routes.values
                    .filter { it.type.isSubway() }
                    .mapNotNull {
                        val rp =
                            objects.routePatterns.values.firstOrNull() { rp -> rp.routeId == it.id }

                        if (rp == null) {
                            return@mapNotNull null
                        }

                        val segments =
                            listOf(
                                RouteSegment(
                                    "fake-id-${rp.id}",
                                    rp.id,
                                    it.id,
                                    objects.trips[rp.representativeTripId]?.stopIds?.let { stopIds
                                        ->
                                        stopIds.map { stopId ->
                                            objects.stops[stopId]?.parentStationId ?: stopId
                                        }
                                    } ?: emptyList(),
                                    mapOf(),
                                )
                            )

                        MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                            it.id,
                            listOf(
                                SegmentedRouteShape(
                                    rp.id,
                                    it.id,
                                    0,
                                    segments,
                                    // All set to a real Red Line polyline so that it actually
                                    // decodes properly.
                                    Shape(
                                        "fake-shape-id${it.id}",
                                        polyline =
                                            "\"}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U\"",
                                    ),
                                )
                            ),
                        )
                    }
            )
    )

    override val state: MutableStateFlow<MapFriendlyRouteResponse> = MutableStateFlow(response)

    override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> {
        onGet()
        return ApiResult.Ok(response)
    }
}

internal class IdleRailRouteShapeRepository : IRailRouteShapeRepository {
    override val state = MutableStateFlow(null)

    override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> {
        return suspendCancellableCoroutine {}
    }
}
