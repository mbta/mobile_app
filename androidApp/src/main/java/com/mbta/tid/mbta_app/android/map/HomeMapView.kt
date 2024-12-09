package com.mbta.tid.mbta_app.android.map

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapEvents
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
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
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.getRailRouteShapes
import com.mbta.tid.mbta_app.android.state.getStopMapData
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteLineData
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.map.StopSourceData
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.map

@OptIn(MapboxExperimental::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeMapView(
    modifier: Modifier = Modifier,
    globalResponse: GlobalResponse?,
    alertsData: AlertsStreamDataResponse?,
    lastNearbyTransitLocation: Position?,
    locationDataManager: LocationDataManager,
    viewportProvider: ViewportProvider,
    currentNavEntry: NavBackStackEntry?,
    handleStopNavigation: (String) -> Unit,
    vehiclesData: List<Vehicle>,
    stopDetailsDepartures: StopDetailsDepartures?,
    stopDetailsFilter: StopDetailsFilter?
) {
    val previousNavEntry: NavBackStackEntry? = rememberPrevious(current = currentNavEntry)

    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }

    val railRouteShapes = getRailRouteShapes()
    var railRouteLineData: List<RouteLineData>? by rememberSaveable { mutableStateOf(null) }
    var stopSourceData: FeatureCollection? by rememberSaveable { mutableStateOf(null) }

    val now = timer(updateInterval = 10.seconds)
    val globalMapData =
        remember(globalResponse, alertsData, now) {
            if (globalResponse != null) {
                GlobalMapData(
                    globalResponse,
                    GlobalMapData.getAlertsByStop(globalResponse, alertsData, now)
                )
            } else {
                null
            }
        }

    val isDarkMode = isSystemInDarkTheme()
    val stopMapData: StopMapResponse? = selectedStop?.let { getStopMapData(stopId = it.id) }

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

    fun updateDisplayedRoutesBasedOnStop() {
        if (railRouteShapes == null || globalResponse == null || stopMapData == null) return

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

    fun refreshRouteLineData() {
        if (railRouteShapes == null || globalResponse == null) return
        railRouteLineData =
            RouteFeaturesBuilder.generateRouteLines(
                railRouteShapes.routesWithSegmentedShapes,
                globalResponse.routes,
                globalResponse.stops,
                globalMapData?.alertsByStop
            )
    }

    fun refreshRouteLineSource() {
        val routeData = railRouteLineData ?: return
        layerManager.run {
            updateRouteSourceData(RouteFeaturesBuilder.buildCollection(routeData).toMapbox())
        }
    }

    fun refreshStopFeatures() {
        val routeLineData = railRouteLineData ?: return
        stopSourceData =
            StopFeaturesBuilder.buildCollection(
                    StopSourceData(selectedStopId = selectedStop?.id),
                    globalMapData?.mapStops.orEmpty(),
                    routeLineData
                )
                .toMapbox()
    }

    fun refreshStopSource() {
        val sourceData = stopSourceData ?: return
        layerManager.run { updateStopSourceData(sourceData) }
    }

    fun handleNearbyNavRestoration() {
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

    fun handleNavChange() {
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

    Box(modifier) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapEvents =
                MapEvents(
                    onStyleLoaded = {
                        layerManager.run {
                            addLayers(if (isDarkMode) ColorPalette.dark else ColorPalette.light)
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
            style = { MapStyle(style = if (isDarkMode) Style.DARK else Style.LIGHT) }
        ) {
            LaunchedEffect(currentNavEntry) { handleNavChange() }
            LaunchedEffect(railRouteShapes, globalResponse, globalMapData) {
                refreshRouteLineData()
            }
            LaunchedEffect(railRouteLineData) {
                refreshRouteLineSource()
                refreshStopFeatures()
            }
            LaunchedEffect(selectedStop) {
                positionViewportToStop()
                refreshStopFeatures()
            }
            LaunchedEffect(stopSourceData) { refreshStopSource() }

            LaunchedEffect(stopMapData) { updateDisplayedRoutesBasedOnStop() }
            LaunchedEffect(stopDetailsFilter) { updateDisplayedRoutesBasedOnStop() }

            val context = LocalContext.current

            val locationProvider = remember { PassthroughLocationProvider() }

            LaunchedEffect(locationDataManager) {
                locationDataManager.currentLocation.collect { location ->
                    if (location != null) {
                        locationProvider.sendLocation(
                            Point.fromLngLat(location.longitude, location.latitude)
                        )
                    }
                }
            }

            MapEffect(true) { map ->
                map.mapboxMap.addOnMapClickListener { point -> handleStopClick(map, point) }
                map.location.setLocationProvider(locationProvider)
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
                locationPermissions.launchMultiplePermissionRequest()
                onStopOrDispose {}
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
                if (layerManager.`object` == null)
                    layerManager.`object` = MapLayerManager(map.mapboxMap, context)
            }

            if (!viewportProvider.isFollowingPuck && lastNearbyTransitLocation != null) {
                CircleAnnotation(
                    point = lastNearbyTransitLocation.toPoint(),
                    circleColorString = "#ba75c7",
                    circleRadius = 10.0
                )
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
            RecenterButton(
                onClick = { viewportProvider.follow() },
                Modifier.align(Alignment.TopEnd).padding(16.dp)
            )
        }
    }
}
