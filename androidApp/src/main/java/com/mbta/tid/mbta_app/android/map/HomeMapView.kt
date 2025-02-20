package com.mbta.tid.mbta_app.android.map

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.appVariant
import com.mbta.tid.mbta_app.android.component.LocationAuthButton
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.SearchResultsViewModel
import com.mbta.tid.mbta_app.android.state.getStopMapData
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.isOverview
import com.mbta.tid.mbta_app.android.util.plus
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun HomeMapView(
    sheetPadding: PaddingValues,
    lastNearbyTransitLocation: Position?,
    nearbyTransitSelectingLocationState: MutableState<Boolean>,
    locationDataManager: LocationDataManager,
    viewportProvider: ViewportProvider,
    currentNavEntry: SheetRoutes?,
    handleStopNavigation: (String) -> Unit,
    vehiclesData: List<Vehicle>,
    stopDetailsDepartures: StopDetailsDepartures?,
    viewModel: IMapViewModel,
    searchResultsViewModel: SearchResultsViewModel,
) {
    var nearbyTransitSelectingLocation by nearbyTransitSelectingLocationState
    val previousNavEntry: SheetRoutes? = rememberPrevious(current = currentNavEntry)

    val coroutineScope = rememberCoroutineScope()
    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }

    val configLoadAttempted = viewModel.configLoadAttempted.collectAsState(initial = false).value
    val railRouteShapes = viewModel.railRouteShapes.collectAsState(initial = null).value
    val stopSourceData = viewModel.stopSourceData.collectAsState(initial = null).value
    val globalResponse = viewModel.globalResponse.collectAsState(initial = null).value
    val railRouteLineData = viewModel.railRouteLineData.collectAsState(initial = null).value
    val selectedVehicle =
        viewModel.selectedVehicle.collectAsState().value.takeIf {
            currentNavEntry is SheetRoutes.StopDetails
        }
    val previousSelectedVehicleId = rememberPrevious(current = selectedVehicle?.id)
    val currentLocation = locationDataManager.currentLocation.collectAsState(initial = null).value

    val now = timer(updateInterval = 300.seconds)
    val globalMapData = viewModel.rememberGlobalMapData(now)
    val isDarkMode = isSystemInDarkTheme()
    val stopMapData: StopMapResponse? = selectedStop?.let { getStopMapData(stopId = it.id) }

    val isNearby = currentNavEntry?.let { it is SheetRoutes.NearbyTransit } ?: true
    val isNearbyNotFollowing = !viewportProvider.isFollowingPuck && isNearby

    val analytics: Analytics = koinInject()
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

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
            analytics.tappedOnStop(stopId)
            handleStopNavigation(stopId)
        }
        return false
    }

    fun positionViewportToStop() {
        val stop = selectedStop ?: return
        viewportProvider.animateTo(stop.position.toMapbox())
    }

    suspend fun updateDisplayedRoutesBasedOnStop() {
        if (globalResponse == null) return
        if (railRouteShapes == null) return
        if (stopMapData == null) return

        val filteredRoutes =
            if (currentNavEntry is SheetRoutes.StopDetails && currentNavEntry.stopFilter != null) {
                RouteFeaturesBuilder.filteredRouteShapesForStop(
                    stopMapData,
                    currentNavEntry.stopFilter,
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
            previousNavEntry is SheetRoutes.NearbyTransit &&
                currentNavEntry is SheetRoutes.StopDetails
        ) {
            viewportProvider.saveNearbyTransitViewport()
        } else if (
            previousNavEntry is SheetRoutes.StopDetails &&
                currentNavEntry is SheetRoutes.NearbyTransit
        ) {
            refreshRouteLineSource()
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    suspend fun handleNavChange() {
        handleNearbyNavRestoration()
        val stopId =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails -> currentNavEntry.stopId
                else -> null
            }
        if (stopId == null) {
            selectedStop = null
            return
        }
        selectedStop = globalResponse?.stops?.get(stopId)
    }

    val cameraZoomFlow =
        remember(viewportProvider.cameraStateFlow) {
            viewportProvider.cameraStateFlow.map { it.zoom }
        }
    val zoomLevel by
        cameraZoomFlow.collectAsState(initial = ViewportProvider.Companion.Defaults.zoom)

    val allVehicles =
        remember(vehiclesData, selectedVehicle) {
            when {
                selectedVehicle == null -> vehiclesData
                else ->
                    vehiclesData.filterNot { it.id == selectedVehicle.id } + listOf(selectedVehicle)
            }
        }

    val pulsingRingColor: Int = colorResource(R.color.key_inverse).toArgb()
    val accuracyRingColor: Int = colorResource(R.color.deemphasized).copy(alpha = 0.1F).toArgb()
    val accuracyRingBorderColor: Int = colorResource(R.color.halo).toArgb()
    val mapState = rememberMapState()
    mapState.gesturesSettings = GesturesSettings {
        rotateEnabled = false
        pitchEnabled = false
    }
    LaunchedEffect(Unit) {
        mapState.styleLoadedEvents.collect {
            layerManager.run {
                addLayers(if (isDarkMode) ColorPalette.dark else ColorPalette.light)
            }
        }
    }
    LaunchedEffect(Unit) {
        mapState.cameraChangedEvents.collect { viewportProvider.updateCameraState(it.cameraState) }
    }

    LaunchedEffect(viewportProvider, sheetPadding) {
        viewportProvider.setSheetPadding(sheetPadding, density, layoutDirection)
    }

    Box(contentAlignment = Alignment.Center) {
        /* Whether loading the config succeeds or not we show the Mapbox Map in case
         * the user has cached tiles on their device.
         */
        if (!configLoadAttempted) {
            Image(
                painterResource(R.drawable.empty_map_grid),
                null,
                Modifier.fillMaxSize().testTag("Empty map grid"),
                contentScale = ContentScale.FillWidth
            )
        } else {
            MapboxMap(
                Modifier.fillMaxSize(),
                compass = {},
                scaleBar = {},
                logo = {
                    Logo(Modifier.clearAndSetSemantics {}, sheetPadding + PaddingValues(8.dp))
                },
                attribution = {
                    Attribution(
                        contentPadding = sheetPadding + PaddingValues(8.dp),
                        alignment = Alignment.BottomEnd
                    )
                },
                mapViewportState = viewportProvider.viewport,
                mapState = mapState,
                style = {
                    MapStyle(
                        style =
                            if (isDarkMode) appVariant.darkMapStyle else appVariant.lightMapStyle
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
                LaunchedEffect(selectedStop) {
                    positionViewportToStop()
                    viewModel.refreshStopFeatures(now, selectedStop)
                }
                LaunchedEffect(stopSourceData) { refreshStopSource() }
                LaunchedEffect(selectedVehicle) {
                    if (
                        selectedVehicle != null && selectedVehicle.id != previousSelectedVehicleId
                    ) {
                        viewportProvider.vehicleOverview(selectedVehicle, selectedStop, density)
                    }
                }

                LaunchedEffect(stopMapData) { updateDisplayedRoutesBasedOnStop() }
                LaunchedEffect(currentNavEntry) { updateDisplayedRoutesBasedOnStop() }

                val locationProvider = remember { PassthroughLocationProvider() }

                MapEffect(true) { map ->
                    map.mapboxMap.addOnMapClickListener { point -> handleStopClick(map, point) }
                    map.location.setLocationProvider(locationProvider)
                    map.location.updateSettings {
                        locationPuck = createDefault2DPuck(withBearing = false)
                        puckBearingEnabled = false
                        enabled = true
                        pulsingEnabled = true
                        pulsingColor = pulsingRingColor
                        pulsingMaxRadius = 24F
                        showAccuracyRing = true
                        this.accuracyRingColor = accuracyRingColor
                        this.accuracyRingBorderColor = accuracyRingBorderColor
                    }
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

                for (vehicle in allVehicles) {
                    val route = globalResponse?.routes?.get(vehicle.routeId) ?: continue
                    val isSelected = vehicle.id == selectedVehicle?.id
                    ViewAnnotation(
                        options =
                            viewAnnotationOptions {
                                selected(isSelected)
                                geometry(Point.fromLngLat(vehicle.longitude, vehicle.latitude))
                                annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                                allowOverlap(true)
                                allowOverlapWithPuck(true)
                                visible(
                                    zoomLevel >= StopLayerGenerator.stopZoomThreshold || isSelected
                                )
                            }
                    ) {
                        VehiclePuck(vehicle = vehicle, route = route, selected = isSelected)
                    }
                }
            }
            val recenterModifier =
                if (isNearby)
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 85.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .statusBarsPadding()
                else Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()

            if (!viewportProvider.isFollowingPuck && isNearby) {
                if (locationDataManager.hasPermission && currentLocation != null) {
                    RecenterButton(
                        onClick = { viewportProvider.follow() },
                        modifier = recenterModifier
                    )
                } else if (!locationDataManager.hasPermission) {
                    LocationAuthButton(
                        locationDataManager,
                        modifier =
                            Modifier.align(Alignment.TopCenter)
                                .padding(top = 85.dp)
                                .statusBarsPadding()
                    )
                }
            }

            if (!viewportProvider.viewport.isOverview && !searchResultsViewModel.expanded) {
                if (selectedVehicle != null) {
                    val routeType =
                        (globalResponse?.routes ?: emptyMap())[selectedVehicle.routeId]?.type
                    if (routeType != null) {
                        TripCenterButton(
                            routeType = routeType,
                            onClick = {
                                viewportProvider.vehicleOverview(
                                    selectedVehicle,
                                    selectedStop,
                                    density
                                )
                            },
                            modifier = recenterModifier
                        )
                    }
                }
            }

            LaunchedEffect(viewportProvider.isManuallyCentering) {
                if (
                    viewportProvider.isManuallyCentering &&
                        currentNavEntry is SheetRoutes.NearbyTransit
                ) {
                    nearbyTransitSelectingLocation = true
                }
            }

            if (nearbyTransitSelectingLocation) {
                Crosshairs()
            }
        }
    }
}
