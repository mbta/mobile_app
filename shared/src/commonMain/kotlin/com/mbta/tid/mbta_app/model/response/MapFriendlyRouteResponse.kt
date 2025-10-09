package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MapFriendlyRouteResponse(
    @SerialName("map_friendly_route_shapes")
    val routesWithSegmentedShapes: List<RouteWithSegmentedShapes>
) {

    @Serializable
    public data class RouteWithSegmentedShapes(
        @SerialName("route_id") val routeId: LineOrRoute.Id,
        @SerialName("route_shapes") val segmentedShapes: List<SegmentedRouteShape>,
    )

    override fun toString(): String = "[MapFriendlyRouteResponse]"
}
