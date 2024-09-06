package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable data class StopDetailsFilter(val routeId: String, val directionId: Int)
