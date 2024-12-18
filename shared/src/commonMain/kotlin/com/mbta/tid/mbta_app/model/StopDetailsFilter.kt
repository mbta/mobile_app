package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

interface RouteDirection {
    val routeId: String
    val directionId: Int
}

@Serializable
data class StopDetailsFilter(override val routeId: String, override val directionId: Int) :
    RouteDirection
