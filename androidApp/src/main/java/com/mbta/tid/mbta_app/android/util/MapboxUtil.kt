package com.mbta.tid.mbta_app.android.util

import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.viewport.CompletionListener
import com.mapbox.maps.plugin.viewport.ViewportStatus
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState
import com.mapbox.maps.plugin.viewport.state.OverviewViewportState
import io.github.dellisd.spatialk.geojson.Position
import kotlin.math.pow
import kotlin.math.round

fun MapViewportState.followPuck(
    zoom: Double? = null,
    completionListener: CompletionListener? = null,
) {
    this.transitionToFollowPuckState(
        followPuckViewportStateOptions =
            FollowPuckViewportStateOptions.Builder()
                .apply {
                    bearing(null)
                    pitch(null)
                    zoom(zoom)
                }
                .build(),
        completionListener = completionListener,
    )
}

val MapViewportState.isFollowingPuck: Boolean
    get() =
        when (val status = this.mapViewportStatus) {
            ViewportStatus.Idle -> false
            is ViewportStatus.State -> status.state is FollowPuckViewportState
            is ViewportStatus.Transition -> status.toState is FollowPuckViewportState
            null -> false
        }

val MapViewportState.isOverview: Boolean
    get() =
        when (val status = this.mapViewportStatus) {
            ViewportStatus.Idle -> false
            is ViewportStatus.State -> status.state is OverviewViewportState
            is ViewportStatus.Transition -> status.toState is OverviewViewportState
            null -> false
        }

fun Double.roundedTo(places: Int): Double {
    val divisor = 10.0.pow(places)
    return round(this * divisor) / divisor
}

fun Point.isRoughlyEqualTo(other: Point) =
    this.latitude().roundedTo(6) == other.latitude().roundedTo(6) &&
        this.longitude().roundedTo(6) == other.longitude().roundedTo(6)

fun Position.isRoughlyEqualTo(other: Position) =
    this.latitude.roundedTo(6) == other.latitude.roundedTo(6) &&
        this.longitude.roundedTo(6) == other.longitude.roundedTo(6)

fun Point.toPosition() = Position(longitude = longitude(), latitude = latitude())

fun Position.toPoint(): Point = Point.fromLngLat(longitude, latitude)
