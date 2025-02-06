package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.serialization.Serializable

interface RouteDirection {
    val routeId: String
    val directionId: Int
}

@Serializable
data class StopDetailsFilter
@DefaultArgumentInterop.Enabled
constructor(
    override val routeId: String,
    override val directionId: Int,
    val autoFilter: Boolean = false
) : RouteDirection

@Serializable
data class TripDetailsFilter(
    val tripId: String,
    val vehicleId: String?,
    val stopSequence: Int?,
    // This is true when manually selecting a vehicle, so that stop details continues focusing on
    // a vehicle selected from the map, even after the prediction for that vehicle isn't displayed
    val selectionLock: Boolean = false
)

@Serializable
data class StopDetailsPageFilters(
    val stopId: String,
    val stopFilter: StopDetailsFilter?,
    val tripFilter: TripDetailsFilter?
)
