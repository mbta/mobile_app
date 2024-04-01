package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.MapFriendlyRouteShape
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteResponse(
    @SerialName("map_friendly_route_shapes") val routeShapes: List<MapFriendlyRouteShape>
)
