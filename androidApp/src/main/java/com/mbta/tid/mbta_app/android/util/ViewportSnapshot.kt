package com.mbta.tid.mbta_app.android.util

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.viewport.CompletionListener

class ViewportSnapshot(val isFollowingPuck: Boolean?, val camera: CameraState?) {
    constructor(
        mapViewportState: MapViewportState
    ) : this(
        isFollowingPuck = mapViewportState.isFollowingPuck,
        camera = mapViewportState.cameraState,
    )

    fun restoreOn(mapViewportState: MapViewportState, completionListener: CompletionListener) {
        if (this.isFollowingPuck == true) {
            mapViewportState.followPuck(
                zoom = camera?.zoom,
                completionListener = completionListener,
            )
        } else if (camera != null) {
            mapViewportState.easeTo(
                CameraOptions.Builder().zoom(camera.zoom).center(camera.center).build(),
                animationOptions = MapAnimationDefaults.options,
                completionListener = completionListener,
            )
        }
    }
}
