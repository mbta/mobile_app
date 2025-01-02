package com.mbta.tid.mbta_app.android.map

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapEvents
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mbta.tid.mbta_app.android.appVariant
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.getStopMapData
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(MapboxExperimental::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeMapView(
    modifier: Modifier = Modifier,
    lastNearbyTransitLocation: Position?,
    nearbyTransitSelectingLocationState: MutableState<Boolean>,
    locationDataManager: LocationDataManager,
    viewportProvider: ViewportProvider,
    currentNavEntry: NavBackStackEntry?,
    handleStopNavigation: (String) -> Unit,
    vehiclesData: List<Vehicle>,
    stopDetailsDepartures: StopDetailsDepartures?,
    stopDetailsFilter: StopDetailsFilter?,
    viewModel: IMapViewModel
) {
    var nearbyTransitSelectingLocation by nearbyTransitSelectingLocationState
    val previousNavEntry: NavBackStackEntry? = rememberPrevious(current = currentNavEntry)

    val coroutineScope = rememberCoroutineScope()
    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }

    val railRouteShapes = viewModel.railRouteShapes.collectAsState(initial = null).value
    val stopSourceData = viewModel.stopSourceData.collectAsState(initial = null).value
    val globalResponse = viewModel.globalResponse.collectAsState(initial = null).value
    val railRouteLineData = viewModel.railRouteLineData.collectAsState(initial = null).value

    val now = timer(updateInterval = 300.seconds)
    val globalMapData = viewModel.rememberGlobalMapData(now)

    val isDarkMode = isSystemInDarkTheme()
    val stopMapData: StopMapResponse? = selectedStop?.let { getStopMapData(stopId = it.id) }

    val isNearby = currentNavEntry?.destination?.route?.contains("NearbyTransit") == true
    val isNearbyNotFollowing = !viewportProvider.isFollowingPuck && isNearby

    fun handleStopClick(map: MapView, point: Point): Boolean {
        val pixel = map.mapboxMap.pixelForCoordinate(point)
        map.mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(pixel),
            RenderedQueryOptions(
                listOf(
                    StopLayerGenerator.stopLayerId,
                    StopLayerGenerator.busLayerId,
                    StopLayerGenerator.stopTouchTargetLayerId
                ),
                null
            )
        ) { result ->
            if (result.isError) {
                Log.e("Map", "Failed handling tap feature query:\n${result.error}")
                return@queryRenderedFeatures
            }
            val tapped = result.value?.firstOrNull() ?: return@queryRenderedFeatures
            val stopId = tapped.queriedFeature.feature.id() ?: return@queryRenderedFeatures
            handleStopNavigation(stopId)
        }
        return false
    }

    fun positionViewportToStop() {
        val stop = selectedStop ?: return
        viewportProvider.animateTo(stop.position.toMapbox())
    }

    suspend fun updateDisplayedRoutesBasedOnStop() {
        val globalResponse = globalResponse ?: return
        val railRouteShapes = railRouteShapes ?: return
        val stopMapData = stopMapData ?: return

        val filteredRoutes =
            if (stopDetailsFilter != null) {
                RouteFeaturesBuilder.filteredRouteShapesForStop(
                    stopMapData,
                    stopDetailsFilter,
                    stopDetailsDepartures
                )
            } else {
                RouteFeaturesBuilder.forRailAtStop(
                    stopMapData.routeShapes,
                    railRouteShapes.routesWithSegmentedShapes,
                    globalResponse.routes
                )
            }
        val newRailData =
            RouteFeaturesBuilder.generateRouteLines(
                filteredRoutes,
                globalResponse.routes,
                globalResponse.stops,
                globalMapData?.alertsByStop
            )
        layerManager.run {
            updateRouteSourceData(RouteFeaturesBuilder.buildCollection(newRailData).toMapbox())
        }
    }

    suspend fun refreshRouteLineSource() {
        val routeData = railRouteLineData ?: return
        layerManager.run {
            updateRouteSourceData(RouteFeaturesBuilder.buildCollection(routeData).toMapbox())
        }
    }

    suspend fun refreshStopSource() {
        val sourceData = stopSourceData ?: return
        layerManager.run { updateStopSourceData(sourceData) }
    }

    suspend fun handleNearbyNavRestoration() {
        if (
            previousNavEntry?.destination?.route?.contains("NearbyTransit") == true &&
                currentNavEntry?.destination?.route?.contains("StopDetails") == true
        ) {
            viewportProvider.saveNearbyTransitViewport()
        } else if (
            previousNavEntry?.destination?.route?.contains("StopDetails") == true &&
                currentNavEntry?.destination?.route?.contains("NearbyTransit") == true
        ) {
            refreshRouteLineSource()
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    suspend fun handleNavChange() {
        handleNearbyNavRestoration()
        val stopId = currentNavEntry?.arguments?.getString("stopId")
        if (stopId == null) {
            selectedStop = null
            return
        }
        selectedStop = globalResponse?.stops?.get(stopId)
    }

    val locationPermissions = locationDataManager.rememberPermissions()

    val cameraZoomFlow =
        remember(viewportProvider.cameraStateFlow) {
            viewportProvider.cameraStateFlow.map { it.zoom }
        }
    val zoomLevel by
        cameraZoomFlow.collectAsState(initial = ViewportProvider.Companion.Defaults.zoom)

    Box(modifier, contentAlignment = Alignment.Center) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapEvents =
                MapEvents(
                    onStyleLoaded = {
                        coroutineScope.launch {
                            layerManager.run {
                                addLayers(if (isDarkMode) ColorPalette.dark else ColorPalette.light)
                            }
                        }
                    },
                    onCameraChanged = { viewportProvider.updateCameraState(it.cameraState) }
                ),
            gesturesSettings =
                GesturesSettings {
                    rotateEnabled = false
                    pitchEnabled = false
                },
            locationComponentSettings =
                LocationComponentSettings(locationPuck = createDefault2DPuck(withBearing = false)) {
                    puckBearingEnabled = false
                    enabled = true
                    pulsingEnabled = false
                },
            compass = {},
            scaleBar = {},
            mapViewportState = viewportProvider.viewport,
            style = {
                MapStyle(
                    style = if (isDarkMode) appVariant.darkMapStyle else appVariant.lightMapStyle
                )
            }
        ) {
            LaunchedEffect(currentNavEntry) { handleNavChange() }
            LaunchedEffect(railRouteShapes, globalResponse, globalMapData) {
                viewModel.refreshRouteLineData(now)
            }
            LaunchedEffect(railRouteLineData) {
                refreshRouteLineSource()
                viewModel.refreshStopFeatures(now, selectedStop)
            }
            LaunchedEffect(Dispatchers.Default, selectedStop) {
                positionViewportToStop()
                viewModel.refreshStopFeatures(now, selectedStop)
            }
            LaunchedEffect(stopSourceData) { refreshStopSource() }

            LaunchedEffect(stopMapData) { updateDisplayedRoutesBasedOnStop() }
            LaunchedEffect(stopDetailsFilter) { updateDisplayedRoutesBasedOnStop() }

            val context = LocalContext.current

            val locationProvider = remember { PassthroughLocationProvider() }

            MapEffect(true) { map ->
                map.mapboxMap.addOnMapClickListener { point -> handleStopClick(map, point) }
                map.location.setLocationProvider(locationProvider)
            }

            LaunchedEffect(locationDataManager) {
                locationDataManager.currentLocation.collect { location ->
                    if (location != null) {
                        locationProvider.sendLocation(
                            Point.fromLngLat(location.longitude, location.latitude)
                        )
                    }
                }
            }

            MapEffect(locationDataManager.hasPermission) { map ->
                if (locationDataManager.hasPermission && viewportProvider.isDefault()) {
                    viewportProvider.follow(
                        DefaultViewportTransitionOptions.Builder().maxDurationMs(0).build()
                    )
                    layerManager.run { resetPuckPosition() }
                }
            }

            LifecycleStartEffect(Unit) {
                onStopOrDispose { viewportProvider.saveCurrentViewport() }
            }

            DisposableMapEffect { map ->
                val listener = ManuallyCenteringListener(viewportProvider)
                map.gestures.addOnMoveListener(listener)
                map.gestures.addOnScaleListener(listener)
                map.gestures.addOnShoveListener(listener)
                onDispose {
                    map.gestures.removeOnMoveListener(listener)
                    map.gestures.removeOnScaleListener(listener)
                    map.gestures.removeOnShoveListener(listener)
                }
            }

            MapEffect { map ->
                if (layerManager.`object` == null) {
                    layerManager.setObject(MapLayerManager(map.mapboxMap, context))
                }
            }

            if (
                isNearbyNotFollowing &&
                    lastNearbyTransitLocation != null &&
                    !nearbyTransitSelectingLocation
            ) {
                ViewAnnotation(
                    options =
                        ViewAnnotationOptions.Builder()
                            .geometry(lastNearbyTransitLocation.toPoint())
                            .annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                            .build()
                ) {
                    Crosshairs()
                }
            }

            for (vehicle in vehiclesData) {
                val route = globalResponse?.routes?.get(vehicle.routeId) ?: continue
                ViewAnnotation(
                    options =
                        viewAnnotationOptions {
                            geometry(Point.fromLngLat(vehicle.longitude, vehicle.latitude))
                            annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                            allowOverlap(true)
                            allowOverlapWithPuck(true)
                            visible(zoomLevel >= StopLayerGenerator.stopZoomThreshold)
                        }
                ) {
                    VehiclePuck(vehicle = vehicle, route = route)
                }
            }
        }

        if (!viewportProvider.isFollowingPuck) {
            val recenterModifier =
                if (isNearby) {
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 86.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                } else {
                    Modifier.align(Alignment.TopEnd).padding(16.dp)
                }

            RecenterButton(
                onClick = {
                    // don't request FINE if we already have COARSE
                    if (!locationPermissions.permissions.any { it.status.isGranted }) {
                        locationPermissions.launchMultiplePermissionRequest()
                    }
                    viewportProvider.follow()
                },
                modifier = recenterModifier
            )
        }

        LaunchedEffect(viewportProvider.isManuallyCentering) {
            if (
                viewportProvider.isManuallyCentering &&
                    currentNavEntry?.destination?.route?.contains("NearbyTransit") == true
            ) {
                nearbyTransitSelectingLocation = true
            }
        }

        if (nearbyTransitSelectingLocation) {
            Crosshairs()
        }
    }
}
