package com.mbta.tid.mbta_app.android.util

import kotlinx.serialization.Serializable

@Serializable data class StopDetailsFilter(val routeId: String, val directionId: Int)
