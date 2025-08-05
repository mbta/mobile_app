package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapFriendlyRouteResponse(
    @SerialName("map_friendly_route_shapes")
    val routesWithSegmentedShapes: List<RouteWithSegmentedShapes>
) {

    @Serializable
    data class RouteWithSegmentedShapes(
        @SerialName("route_id") val routeId: String,
        @SerialName("route_shapes") val segmentedShapes: List<SegmentedRouteShape>,
    )

    override fun toString() = "[MapFriendlyRouteResponse]"
}
