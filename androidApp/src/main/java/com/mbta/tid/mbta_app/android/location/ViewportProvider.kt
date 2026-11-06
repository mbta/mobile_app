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
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState as RawMapViewportState
import com.mapbox.maps.plugin.viewport.CompletionListener
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
import com.mbta.tid.mbta_app.map.MapViewportState
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.ViewportManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.spatialk.geojson.Position

interface IViewportProvider {
    val state: MapViewportState
    val isCurrentlyPanning: Boolean
    val isFollowingPuck: Boolean
    val isVehicleOverview: Boolean

    var cameraStateFlow: Flow<CameraState>

    var cameraStateFlowDebounced: Flow<CameraState>

    fun getViewportImmediate(): RawMapViewportState

    suspend fun setSheetPadding(
        sheetPadding: PaddingValues,
        density: Density,
        layoutDirection: LayoutDirection,
    )

    fun setIsCurrentlyPanning(isCurrentlyPanning: Boolean)

    suspend fun follow(transitionAnimationDuration: Long? = null)

    suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float?)

    suspend fun stopCenter(stop: Stop)

    suspend fun applyState(state: MapViewportState, instantly: Boolean)

    fun isDefault(): Boolean

    suspend fun panToDefaultCenter()

    fun updateCameraState(location: Location?)

    fun updateCameraState(state: CameraState)

    fun saveCurrentViewport()

    suspend fun saveNearbyTransitViewport()

    suspend fun restoreNearbyTransitViewport()
}

class ViewportProvider(
    private var _rawViewport: RawMapViewportState,
) : IViewportProvider, ViewportManager {
    override var state by
        mutableStateOf<MapViewportState>(
            MapViewportState.ManuallyPanned(toDefault = true, currentlyPanning = false),
        )
        private set

    override val isCurrentlyPanning: Boolean
        get() {
            val state = this.state
            return state is MapViewportState.ManuallyPanned && state.currentlyPanning
        }

    override val isFollowingPuck: Boolean
        get() = state == MapViewportState.FollowingPuck

    override val isVehicleOverview: Boolean
        get() = state is MapViewportState.VehicleOverview

    private var savedNearbyTransitViewport: ViewportSnapshot? = null
    private var savedNearbyTransitState: MapViewportState? = null

    private var _cameraState =
        MutableStateFlow(
            // For some reason, the initial state of the viewport doesn't apply immediately, so the
            // viewport may be completely uninitialized here; if it is, use the defaults.
            getViewportImmediate().cameraState.takeUnless { it?.zoom == 0.0 }
                ?: CameraState(
                    MapDefaults.center.toMapbox(),
                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                    MapDefaults.zoom,
                    0.0,
                    0.0,
                ),
        )
    override var cameraStateFlow =
        _cameraState.asStateFlow().distinctUntilChanged { old, new ->
            old.center.isRoughlyEqualTo(new.center)
        }
    @OptIn(FlowPreview::class)
    override var cameraStateFlowDebounced = cameraStateFlow.sample(0.5.seconds)

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
    suspend fun <T> withViewport(operation: suspend (RawMapViewportState) -> T): T? {
        return viewportLock.withLock {
            // the follow animation sometimes completely drops its CompletionListener, like if it’s
            // interrupted by switching to the More tab, so we need to release the lock by timing
            // out if the operation gets stuck
            withTimeoutOrNull(MapAnimationDefaults.duration * 2) {
                withContext(Dispatchers.Default) {
                    ensureActive()
                    operation(_rawViewport)
                }
            }
        }
    }

    /**
     * Creates a [CompletionListener] to ensure that the animation is not interrupted. Runs on a
     * background thread.
     */
    private suspend fun animateViewport(
        operation: (RawMapViewportState, CompletionListener) -> Unit,
    ) {
        withViewport { viewport ->
            suspendCancellableCoroutine { continuation ->
                runningAnimation = continuation
                operation(viewport, CompletionListener { continuation.tryResume() })
                // interrupting the animation can only be done from the UI thread, and it can look
                // bad, so we don’t interrupt the animation on cancel
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
        applyState(MapViewportState.FollowingPuck, instantly = transitionAnimationDuration == 0L)
    }

    override suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float?) {
        applyState(MapViewportState.VehicleOverview(vehicle.position, stop?.position, density))
    }

    override suspend fun stopCenter(stop: Stop) {
        applyState(MapViewportState.StopCenter(stop.position))
    }

    override suspend fun applyState(state: MapViewportState, instantly: Boolean) {
        this.state = state
        when (state) {
            MapViewportState.FollowingPuck -> {
                val transitionOptions =
                    if (instantly)
                        DefaultViewportTransitionOptions.Builder().maxDurationMs(0).build()
                    else Defaults.viewportTransition
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
                        completionListener = completionListener,
                    )
                }
            }
            is MapViewportState.ManuallyPanned -> {
                if (state.toDefault) {
                    animateTo(MapDefaults.center, zoom = 13.75)
                    this.state = state.copy(currentlyPanning = false)
                }
            }
            is MapViewportState.StopCenter -> {
                animateTo(state.stopPosition)
            }
            is MapViewportState.VehicleOverview -> {
                val targetStopPosition = state.targetStopPosition
                val density = state.density
                if (targetStopPosition == null) {
                    animateTo(state.vehiclePosition)
                } else if (density != null) {
                    animateViewport { viewport, completionListener ->
                        viewport.transitionToOverviewState(
                            OverviewViewportStateOptions.Builder()
                                .padding(lastEdgeInsets)
                                .geometry(
                                    MultiPoint.fromLngLats(
                                        listOf(
                                            state.vehiclePosition.toMapbox(),
                                            targetStopPosition.toMapbox(),
                                        ),
                                    ),
                                )
                                .geometryPadding(Defaults.overviewPadding(density))
                                .maxZoom(16.0)
                                .build(),
                            Defaults.viewportTransition,
                            completionListener,
                        )
                    }
                }
            }
        }
    }

    // if camera state's center is null, then treat it like it is at the default center
    override fun isDefault() = (state as? MapViewportState.ManuallyPanned)?.toDefault ?: false

    override suspend fun panToDefaultCenter() {
        applyState(MapViewportState.ManuallyPanned(toDefault = true, currentlyPanning = true))
    }

    private suspend fun animateTo(
        position: Position,
        zoom: Double? = null,
    ) {
        animateViewport { viewport, completionListener ->
            viewport.easeTo(
                cameraOptions =
                    CameraOptions.Builder().center(position.toMapbox()).zoom(zoom).build(),
                animationOptions = MapAnimationDefaults.options,
                completionListener = completionListener,
            )
        }
    }

    override fun updateCameraState(location: Location?) {
        val latitude = location?.latitude ?: return
        val longitude = location.longitude
        updateCameraState(
            CameraState(
                Point.fromLngLat(longitude, latitude),
                EdgeInsets(0.0, 0.0, 0.0, 0.0),
                MapDefaults.zoom,
                0.0,
                0.0,
            ),
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
        savedNearbyTransitState?.let { state = it }
        try {
            animateViewport { viewport, completionListener ->
                savedNearbyTransitViewport?.restoreOn(viewport, completionListener)
            }
        } finally {
            savedNearbyTransitViewport = null
            savedNearbyTransitState = null
        }
    }

    override fun setIsCurrentlyPanning(isCurrentlyPanning: Boolean) {
        state =
            MapViewportState.ManuallyPanned(
                toDefault = false,
                currentlyPanning = isCurrentlyPanning,
            )
        if (isCurrentlyPanning) {
            runningAnimation.tryResume()
        }
    }

    private companion object {
        object Defaults {
            val viewportTransition =
                DefaultViewportTransitionOptions.Builder()
                    .maxDurationMs(MapAnimationDefaults.duration.inWholeMilliseconds)
                    .build()

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
