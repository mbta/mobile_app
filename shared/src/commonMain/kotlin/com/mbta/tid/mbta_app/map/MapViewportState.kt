package com.mbta.tid.mbta_app.map

import org.maplibre.spatialk.geojson.Position

public sealed class MapViewportState {
    public data object FollowingPuck : MapViewportState()

    public data class ManuallyPanned(val toDefault: Boolean, val currentlyPanning: Boolean) :
        MapViewportState()

    public data class StopCenter(val stopPosition: Position) : MapViewportState()

    public data class VehicleOverview(
        val vehiclePosition: Position,
        val targetStopPosition: Position?,
        val density: Float?,
    ) : MapViewportState()
}
