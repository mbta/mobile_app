package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Route
import kotlinx.serialization.Serializable

@Serializable data class RouteResponse(val routes: List<Route>)
