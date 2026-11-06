package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.map.MapViewportState

public interface ViewportManager {
    public suspend fun applyState(state: MapViewportState, instantly: Boolean = false)

    public suspend fun saveNearbyTransitViewport()

    public suspend fun restoreNearbyTransitViewport()

    public fun isDefault(): Boolean
}
