package com.mbta.tid.mbta_app.android.map

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapEvents
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.followPuck
import com.mbta.tid.mbta_app.android.util.getRailRouteShapes
import com.mbta.tid.mbta_app.android.util.isFollowingPuck
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
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@OptIn(MapboxExperimental::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeMapView(
    modifier: Modifier = Modifier,
    mapViewportState: MapViewportState,
    globalResponse: GlobalResponse?,
    alertsData: AlertsStreamDataResponse?,
    lastNearbyTransitLocation: Position?,
    navController: NavHostController,
) {

    val currentNavEntry: NavBackStackEntry? by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)
    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    val locationPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    val railRouteShapes = getRailRouteShapes()
    var railRouteLineData: List<RouteLineData>? by rememberSaveable { mutableStateOf(null) }
    var stopSourceData: FeatureCollection? by rememberSaveable { mutableStateOf(null) }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }

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
            navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
                popUpTo(SheetRoutes.NearbyTransit)
            }
        }
        return false
    }

    fun positionViewportToStop() {
        if (selectedStop != null) {
            mapViewportState.easeTo(
                cameraOptions =
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(selectedStop!!.longitude, selectedStop!!.latitude))
                        .zoom(MapDefaults.stopPageZoom)
                        .build(),
                animationOptions = MapAnimationDefaults.options
            )
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

    Box(modifier) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapEvents =
                MapEvents(
                    onStyleLoaded = {
                        layerManager.run {
                            addLayers(if (isDarkMode) ColorPalette.dark else ColorPalette.light)
                        }
                    }
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
            mapViewportState = mapViewportState,
            style = { MapStyle(style = if (isDarkMode) Style.DARK else Style.LIGHT) }
        ) {
            LaunchedEffect(currentNavEntry) {
                val stopId = currentNavEntry?.arguments?.getString("stopId")
                if (stopId == null) {
                    selectedStop = null
                    return@LaunchedEffect
                }
                selectedStop = globalResponse?.stops?.get(stopId)
            }

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

            val context = LocalContext.current

            MapEffect(true) { map ->
                mapViewportState.followPuck()
                map.mapboxMap.addOnMapClickListener { point -> handleStopClick(map, point) }
            }

            MapEffect { map ->
                if (layerManager.`object` == null)
                    layerManager.`object` = MapLayerManager(map.mapboxMap, context)
            }

            if (!mapViewportState.isFollowingPuck && lastNearbyTransitLocation != null) {
                CircleAnnotation(
                    point = lastNearbyTransitLocation.toPoint(),
                    circleColorString = "#ba75c7",
                    circleRadius = 10.0
                )
            }
        }

        if (!mapViewportState.isFollowingPuck || !locationPermissionState.status.isGranted) {
            RecenterButton(
                onClick = {
                    locationPermissionState.launchPermissionRequest()
                    mapViewportState.followPuck()
                },
                Modifier.align(Alignment.TopEnd).padding(16.dp)
            )
        }
    }
}
