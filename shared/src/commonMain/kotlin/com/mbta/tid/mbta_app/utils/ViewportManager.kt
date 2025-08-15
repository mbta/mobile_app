package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle

public interface ViewportManager {
    public suspend fun saveNearbyTransitViewport()

    public suspend fun restoreNearbyTransitViewport()

    public suspend fun stopCenter(stop: Stop)

    public suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float)

    public suspend fun follow(transitionAnimationDuration: Long?)

    public suspend fun isDefault(): Boolean
}
