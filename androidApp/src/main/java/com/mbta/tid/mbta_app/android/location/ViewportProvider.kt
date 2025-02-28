package com.mbta.tid.mbta_app.android.location

import android.location.Location
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
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
import com.mbta.tid.mbta_app.android.util.isOverview
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.map.MapDefaults
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

class ViewportProvider(var viewport: MapViewportState, isManuallyCentering: Boolean = false) {
    var isManuallyCentering by mutableStateOf(isManuallyCentering)
    var isFollowingPuck by mutableStateOf(viewport.isFollowingPuck)
    var isVehicleOverview by mutableStateOf(viewport.isOverview)

    private var savedNearbyTransitViewport: ViewportSnapshot? = null

    private var _cameraState =
        MutableStateFlow(
            // For some reason, the initial state of the viewport doesn't apply immediately, so the
            // viewport may be completely uninitialized here; if it is, use the defaults.
            viewport.cameraState.takeUnless { it?.zoom == 0.0 }
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

    private var lastEdgeInsets: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)

    suspend fun setSheetPadding(
        sheetPadding: PaddingValues,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val insets =
            with(density) {
                EdgeInsets(
                    sheetPadding.calculateTopPadding().toPx().toDouble(),
                    sheetPadding.calculateLeftPadding(layoutDirection).toPx().toDouble(),
                    sheetPadding.calculateBottomPadding().toPx().toDouble(),
                    sheetPadding.calculateRightPadding(layoutDirection).toPx().toDouble()
                )
            }

        withContext(Dispatchers.Main) { viewport.setCameraOptions { padding(insets) } }

        lastEdgeInsets = insets
    }

    fun follow(
        defaultTransitionOptions: DefaultViewportTransitionOptions = Defaults.viewportTransition
    ) {
        isFollowingPuck = true
        isVehicleOverview = false
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
        isVehicleOverview = true
        isFollowingPuck = false
        if (stop == null) {
            animateTo(vehicle.position.toMapbox())
        } else {
            animateToOverview(
                OverviewViewportStateOptions.Builder()
                    .padding(lastEdgeInsets)
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

    fun isDefault() = viewport.cameraState?.center?.isRoughlyEqualTo(Defaults.center) == true

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

    fun saveNearbyTransitViewport() {
        savedNearbyTransitViewport = ViewportSnapshot(viewport)
    }

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
                        95.dp.toPx().toDouble(),
                        50.dp.toPx().toDouble(),
                        75.dp.toPx().toDouble(),
                        50.dp.toPx().toDouble()
                    )
                }
        }
    }
}
