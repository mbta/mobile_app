package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle

interface ViewportManager {
    suspend fun saveNearbyTransitViewport()

    suspend fun restoreNearbyTransitViewport()

    suspend fun stopCenter(stop: Stop)

    suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float)

    suspend fun follow(transitionAnimationDuration: Long?)

    suspend fun isDefault(): Boolean
}
