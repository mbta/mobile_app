package com.mbta.tid.mbta_app.android.util

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.viewport.ViewportStatus
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState
import io.github.dellisd.spatialk.geojson.Position

@OptIn(MapboxExperimental::class)
fun MapViewportState.followPuck() {
    this.transitionToFollowPuckState(
        followPuckViewportStateOptions =
            FollowPuckViewportStateOptions.Builder()
                .apply {
                    bearing(null)
                    pitch(null)
                    zoom(null)
                }
                .build()
    )
}

@OptIn(MapboxExperimental::class)
val MapViewportState.isFollowingPuck: Boolean
    get() =
        when (val status = this.mapViewportStatus) {
            ViewportStatus.Idle -> false
            is ViewportStatus.State -> status.state is FollowPuckViewportState
            is ViewportStatus.Transition -> status.toState is FollowPuckViewportState
        }

fun Point.toPosition() = Position(longitude = longitude(), latitude = latitude())

fun Position.toPoint(): Point = Point.fromLngLat(longitude, latitude)
