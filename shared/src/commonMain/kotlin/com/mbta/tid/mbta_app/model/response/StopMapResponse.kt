package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Stop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class StopMapResponse(
    @SerialName("map_friendly_route_shapes")
    val routeShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
    @SerialName("child_stops") internal val childStops: Map<String, Stop>,
) {
    override fun toString(): String = "[StopMapResponse]"
}
