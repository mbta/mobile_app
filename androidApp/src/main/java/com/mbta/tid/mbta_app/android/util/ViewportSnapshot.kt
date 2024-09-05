package com.mbta.tid.mbta_app.android.util

import com.mapbox.maps.CameraState
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.toCameraOptions

@OptIn(MapboxExperimental::class)
class ViewportSnapshot(val isFollowingPuck: Boolean?, val camera: CameraState?) {
    constructor(
        mapViewportState: MapViewportState
    ) : this(
        isFollowingPuck = mapViewportState.isFollowingPuck,
        camera = mapViewportState.cameraState
    )

    fun restoreOn(mapViewportState: MapViewportState) {
        if (this.isFollowingPuck == true) {
            mapViewportState.followPuck(zoom = camera?.zoom)
        } else if (camera != null) {
            mapViewportState.easeTo(
                camera.toCameraOptions(),
                animationOptions = MapAnimationDefaults.options
            )
        }
    }
}
