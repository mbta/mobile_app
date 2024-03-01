package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteResponse(
    val routes: List<Route>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    val shapes: Map<String, Shape>,
    val trips: Map<String, Trip>
)
