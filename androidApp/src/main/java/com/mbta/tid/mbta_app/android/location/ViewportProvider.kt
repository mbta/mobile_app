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
import com.mapbox.maps.plugin.viewport.CompletionListener
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
import com.mbta.tid.mbta_app.utils.ViewportManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface IViewportProvider {
    var isManuallyCentering: Boolean
    var isFollowingPuck: Boolean
    var isVehicleOverview: Boolean
    var isAnimating: Boolean

    var cameraStateFlow: Flow<CameraState>

    fun getViewportImmediate(): MapViewportState

    suspend fun setSheetPadding(
        sheetPadding: PaddingValues,
        density: Density,
        layoutDirection: LayoutDirection,
    )

    fun setIsManuallyCentering(isManuallyCentering: Boolean)

    suspend fun follow(transitionAnimationDuration: Long? = null)

    suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float)

    suspend fun stopCenter(stop: Stop)

    suspend fun isDefault(): Boolean

    suspend fun panToDefaultCenter()

    fun updateCameraState(location: Location?)

    fun updateCameraState(state: CameraState)

    fun saveCurrentViewport()

    suspend fun saveNearbyTransitViewport()

    suspend fun restoreNearbyTransitViewport()
}

class ViewportProvider(
    private var _rawViewport: MapViewportState,
    isManuallyCentering: Boolean = false,
) : IViewportProvider, ViewportManager {
    override var isManuallyCentering by mutableStateOf(isManuallyCentering)
    override var isFollowingPuck by mutableStateOf(_rawViewport.isFollowingPuck)
    override var isVehicleOverview by mutableStateOf(_rawViewport.isOverview)
    override var isAnimating by mutableStateOf(false)

    private var savedNearbyTransitViewport: ViewportSnapshot? = null

    private var _cameraState =
        MutableStateFlow(
            // For some reason, the initial state of the viewport doesn't apply immediately, so the
            // viewport may be completely uninitialized here; if it is, use the defaults.
            getViewportImmediate().cameraState.takeUnless { it?.zoom == 0.0 }
                ?: CameraState(
                    Defaults.center,
                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                    Defaults.zoom,
                    0.0,
                    0.0,
                )
        )
    override var cameraStateFlow =
        _cameraState.asStateFlow().distinctUntilChanged { old, new ->
            old.center.isRoughlyEqualTo(new.center)
        }

    private var lastEdgeInsets: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)

    private val viewportLock = Mutex()
    private var runningAnimation: Continuation<Unit>? = null

    private fun Continuation<Unit>?.tryResume() =
        try {
            this?.resume(Unit)
            runningAnimation = null
        } catch (_: IllegalStateException) {}

    /**
     * Gets the current state of the viewport without waiting for any in-progress animations to
     * finish. If possible, prefer reading and writing via [withViewport] to avoid synchronization
     * issues.
     */
    override fun getViewportImmediate() = _rawViewport

    /**
     * Queues an operation to run on the viewport once all previously queued animations have
     * finished. Runs on a background thread.
     */
    suspend fun <T> withViewport(operation: suspend (MapViewportState) -> T): T {
        return viewportLock.withLock {
            withContext(Dispatchers.Default) {
                ensureActive()
                operation(_rawViewport)
            }
        }
    }

    /**
     * Creates a [CompletionListener] to ensure that the animation is not interrupted. Runs on a
     * background thread.
     */
    private suspend fun animateViewport(operation: (MapViewportState, CompletionListener) -> Unit) {
        withViewport { viewport ->
            suspendCoroutine { continuation ->
                runningAnimation = continuation
                operation(viewport, CompletionListener { continuation.tryResume() })
            }
        }
    }

    override suspend fun setSheetPadding(
        sheetPadding: PaddingValues,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        val insets =
            with(density) {
                EdgeInsets(
                    sheetPadding.calculateTopPadding().toPx().toDouble(),
                    sheetPadding.calculateLeftPadding(layoutDirection).toPx().toDouble(),
                    sheetPadding.calculateBottomPadding().toPx().toDouble(),
                    sheetPadding.calculateRightPadding(layoutDirection).toPx().toDouble(),
                )
            }

        withViewport { viewport ->
            withContext(Dispatchers.Main) {
                ensureActive()
                viewport.setCameraOptions { padding(insets) }
            }
            lastEdgeInsets = insets
        }
    }

    override suspend fun follow(transitionAnimationDuration: Long?) {
        val transitionOptions =
            transitionAnimationDuration?.let {
                DefaultViewportTransitionOptions.Builder()
                    .maxDurationMs(transitionAnimationDuration)
                    .build()
            } ?: Defaults.viewportTransition
        isFollowingPuck = true
        isVehicleOverview = false
        isAnimating = true
        animateViewport { viewport, completionListener ->
            viewport.transitionToFollowPuckState(
                followPuckViewportStateOptions =
                    FollowPuckViewportStateOptions.Builder()
                        .apply {
                            bearing(null)
                            pitch(null)
                            zoom(_cameraState.value.zoom)
                        }
                        .build(),
                defaultTransitionOptions = transitionOptions,
                completionListener,
            )
        }
        isAnimating = false
    }

    override suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float) {
        isVehicleOverview = true
        isFollowingPuck = false
        if (stop == null) {
            animateTo(vehicle.position.toMapbox())
        } else {
            animateToOverview({
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
            })
        }
    }

    override suspend fun stopCenter(stop: Stop) {
        isFollowingPuck = false
        isVehicleOverview = false
        animateTo(stop.position.toMapbox())
    }

    // if camera state's center is null, then treat it like it is at the default center
    override suspend fun isDefault() = withViewport { viewport ->
        viewport.cameraState?.center?.isRoughlyEqualTo(Defaults.center) != false
    }

    override suspend fun panToDefaultCenter() {
        isManuallyCentering = true
        isFollowingPuck = false
        animateTo(Defaults.center, zoom = 13.75)
    }

    private suspend fun animateTo(
        coordinates: Point,
        animation: MapAnimationOptions = MapAnimationDefaults.options,
        zoom: Double? = null,
    ) {
        isAnimating = true
        animateToCamera(
            options =
                CameraOptions.Builder()
                    .center(coordinates)
                    .zoom(zoom ?: _cameraState.value.zoom)
                    .build(),
            animation = animation,
        )
        isAnimating = false
    }

    private suspend fun animateToCamera(options: CameraOptions, animation: MapAnimationOptions) {
        animateViewport { viewport, completionListener ->
            viewport.easeTo(
                options,
                animation,
                { isFinished ->
                    isManuallyCentering = false
                    completionListener.onComplete(isFinished)
                },
            )
        }
    }

    private suspend fun animateToOverview(
        options: () -> OverviewViewportStateOptions,
        defaultTransitionOptions: DefaultViewportTransitionOptions = Defaults.viewportTransition,
    ) {
        isAnimating = true
        animateViewport { viewport, completionListener ->
            viewport.transitionToOverviewState(
                options(),
                defaultTransitionOptions,
                completionListener,
            )
        }
        isAnimating = false
    }

    override fun updateCameraState(location: Location?) {
        val latitude = location?.latitude ?: return
        val longitude = location.longitude
        updateCameraState(
            CameraState(
                Point.fromLngLat(longitude, latitude),
                EdgeInsets(0.0, 0.0, 0.0, 0.0),
                Defaults.zoom,
                0.0,
                0.0,
            )
        )
    }

    override fun updateCameraState(state: CameraState) {
        _cameraState.tryEmit(state)
    }

    override fun saveCurrentViewport() {
        val camera = _cameraState.value
        if (getViewportImmediate().isFollowingPuck) {
            getViewportImmediate().followPuck(zoom = camera.zoom)
        } else {
            getViewportImmediate().setCameraOptions {
                center(camera.center)
                zoom(camera.zoom)
            }
        }
    }

    override suspend fun saveNearbyTransitViewport() {
        withViewport { viewport -> savedNearbyTransitViewport = ViewportSnapshot(viewport) }
    }

    override suspend fun restoreNearbyTransitViewport() {
        isFollowingPuck = savedNearbyTransitViewport?.isFollowingPuck ?: false
        isAnimating = true
        animateViewport { viewport, completionListener ->
            savedNearbyTransitViewport?.restoreOn(viewport, completionListener)
        }
        isAnimating = false
        savedNearbyTransitViewport = null
    }

    override fun setIsManuallyCentering(isManuallyCentering: Boolean) {
        this.isManuallyCentering = isManuallyCentering
        if (isManuallyCentering) {
            runningAnimation.tryResume()
            isAnimating = false
            isFollowingPuck = false
            isVehicleOverview = false
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

            fun overviewPadding(density: Float) =
                EdgeInsets(
                    (95.dp * density).value.toDouble(),
                    (50.dp * density).value.toDouble(),
                    (75.dp * density).value.toDouble(),
                    (50.dp * density).value.toDouble(),
                )
        }
    }
}
