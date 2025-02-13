package com.mbta.tid.mbta_app.android.location

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.data.OverviewViewportStateOptions
import com.mbta.tid.mbta_app.android.map.toMapbox
import com.mbta.tid.mbta_app.android.util.MapAnimationDefaults
import com.mbta.tid.mbta_app.android.util.ViewportSnapshot
import com.mbta.tid.mbta_app.android.util.followPuck
import com.mbta.tid.mbta_app.android.util.isFollowingPuck
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.map.MapDefaults
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ViewportProvider
@OptIn(MapboxExperimental::class)
constructor(var viewport: MapViewportState, isManuallyCentering: Boolean = false) {
    var isManuallyCentering by mutableStateOf(isManuallyCentering)
    @OptIn(MapboxExperimental::class)
    var isFollowingPuck by mutableStateOf(viewport.isFollowingPuck)

    private var savedNearbyTransitViewport: ViewportSnapshot? = null

    @OptIn(MapboxExperimental::class)
    private var _cameraState =
        MutableStateFlow(
            // For some reason, the initial state of the viewport doesn't apply immediately, so the
            // viewport may be completely uninitialized here; if it is, use the defaults.
            viewport.cameraState.takeUnless { it.zoom == 0.0 }
                ?: CameraState(
                    Defaults.center,
                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                    Defaults.zoom,
                    0.0,
                    0.0
                )
        )
    var cameraStateFlow =
        _cameraState.asStateFlow().distinctUntilChanged { old, new ->
            old.center.isRoughlyEqualTo(new.center)
        }

    @OptIn(MapboxExperimental::class)
    fun follow(
        defaultTransitionOptions: DefaultViewportTransitionOptions = Defaults.viewportTransition
    ) {
        isFollowingPuck = true
        this.viewport.transitionToFollowPuckState(
            followPuckViewportStateOptions =
                FollowPuckViewportStateOptions.Builder()
                    .apply {
                        bearing(null)
                        pitch(null)
                        zoom(_cameraState.value.zoom)
                    }
                    .build(),
            defaultTransitionOptions = defaultTransitionOptions
        )
    }

    fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Density) {
        if (stop == null) {
            animateTo(vehicle.position.toMapbox())
        } else {
            animateToOverview(
                OverviewViewportStateOptions.Builder()
                    .geometry(
                        MultiPoint.fromLngLats(
                            listOf(vehicle.position.toMapbox(), stop.position.toMapbox())
                        )
                    )
                    .geometryPadding(Defaults.overviewPadding(density))
                    .maxZoom(16.0)
                    .build()
            )
        }
    }

    @OptIn(MapboxExperimental::class)
    fun isDefault() = viewport.cameraState.center.isRoughlyEqualTo(Defaults.center)

    fun animateTo(
        coordinates: Point,
        animation: MapAnimationOptions = MapAnimationDefaults.options,
        zoom: Double? = null
    ) {
        isFollowingPuck = false
        animateToCamera(
            options =
                CameraOptions.Builder()
                    .center(coordinates)
                    .zoom(zoom ?: _cameraState.value.zoom)
                    .build(),
            animation = animation
        )
    }

    @OptIn(MapboxExperimental::class)
    fun animateToCamera(
        options: CameraOptions,
        animation: MapAnimationOptions = MapAnimationDefaults.options
    ) {
        this.viewport.easeTo(
            options,
            animation,
            completionListener = { isManuallyCentering = false }
        )
    }

    @OptIn(MapboxExperimental::class)
    fun animateToOverview(
        options: OverviewViewportStateOptions,
        defaultTransitionOptions: DefaultViewportTransitionOptions = Defaults.viewportTransition
    ) {
        this.viewport.transitionToOverviewState(options, defaultTransitionOptions)
    }

    fun updateCameraState(location: Location?) {
        val latitude = location?.latitude ?: return
        val longitude = location.longitude
        updateCameraState(
            CameraState(
                Point.fromLngLat(longitude, latitude),
                EdgeInsets(0.0, 0.0, 0.0, 0.0),
                Defaults.zoom,
                0.0,
                0.0
            )
        )
    }

    fun updateCameraState(state: CameraState) {
        _cameraState.tryEmit(state)
    }

    @OptIn(MapboxExperimental::class)
    fun saveCurrentViewport() {
        val camera = _cameraState.value
        if (viewport.isFollowingPuck) {
            viewport.followPuck(zoom = camera.zoom)
        } else {
            viewport.setCameraOptions {
                center(camera.center)
                zoom(camera.zoom)
            }
        }
    }

    @OptIn(MapboxExperimental::class)
    fun saveNearbyTransitViewport() {
        savedNearbyTransitViewport = ViewportSnapshot(viewport)
    }

    @OptIn(MapboxExperimental::class)
    fun restoreNearbyTransitViewport() {
        // TODO preserve zoom
        savedNearbyTransitViewport?.restoreOn(viewport)
        savedNearbyTransitViewport = null
    }

    fun setIsManuallyCentering(isManuallyCentering: Boolean) {
        this.isManuallyCentering = isManuallyCentering
        if (isManuallyCentering) {
            isFollowingPuck = false
        }
    }

    companion object {
        object Defaults {
            val viewportTransition =
                DefaultViewportTransitionOptions.Builder()
                    .maxDurationMs(MapAnimationDefaults.duration)
                    .build()
            val center: Point = Point.fromLngLat(-71.0601, 42.3575)
            val zoom = MapDefaults.defaultZoomThreshold

            fun overviewPadding(density: Density) =
                with(density) {
                    EdgeInsets(
                        75.dp.toPx().toDouble(),
                        50.dp.toPx().toDouble(),
                        75.dp.toPx().toDouble(),
                        50.dp.toPx().toDouble()
                    )
                }
        }
    }
}
