package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.mapbox.maps.CameraBoundsOptions
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
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.appVariant
import com.mbta.tid.mbta_app.android.component.LocationAuthButton
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.location.IViewportProvider
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.getStopMapData
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.getStopIdAt
import com.mbta.tid.mbta_app.android.util.plus
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomeMapView(
    sheetPadding: PaddingValues,
    lastNearbyTransitLocation: Position?,
    nearbyTransitSelectingLocationState: MutableState<Boolean>,
    locationDataManager: LocationDataManager,
    viewportProvider: IViewportProvider,
    currentNavEntry: SheetRoutes?,
    handleStopNavigation: (String) -> Unit,
    handleVehicleTap: (Vehicle) -> Unit,
    vehiclesData: List<Vehicle>,
    routeCardData: List<RouteCardData>?,
    viewModel: IMapViewModel,
    isSearchExpanded: Boolean,
    mapboxConfigManager: IMapboxConfigManager = koinInject(),
) {
    var nearbyTransitSelectingLocation by nearbyTransitSelectingLocationState
    val previousNavEntry: SheetRoutes? = rememberPrevious(current = currentNavEntry)

    val coroutineScope = rememberCoroutineScope()
    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    val selectedStop by viewModel.selectedStop.collectAsState(null)
    val stopFilter by viewModel.stopFilter.collectAsState(null)

    val configLoadAttempted by
        mapboxConfigManager.configLoadAttempted.collectAsState(initial = false)
    val railRouteShapes by viewModel.railRouteShapes.collectAsState(initial = null)
    val stopSourceData by viewModel.stopSourceData.collectAsState(initial = null)
    val globalResponse by viewModel.globalResponse.collectAsState(initial = null)
    val railRouteSourceData by viewModel.railRouteSourceData.collectAsState(initial = null)
    val showRecenterButton by viewModel.showRecenterButton.collectAsState(initial = false)
    val showTripCenterButton by viewModel.showTripCenterButton.collectAsState(initial = false)
    val selectedVehicle =
        viewModel.selectedVehicle.collectAsState().value.takeIf {
            currentNavEntry is SheetRoutes.StopDetails
        }
    val previousSelectedVehicleId = rememberPrevious(current = selectedVehicle?.id)
    val currentLocation by locationDataManager.currentLocation.collectAsState(initial = null)
    val now by timer(updateInterval = 300.seconds)
    val globalMapData by viewModel.globalMapData.collectAsState(null)
    val isDarkMode = isSystemInDarkTheme()
    val stopMapData: StopMapResponse? = selectedStop?.let { getStopMapData(stopId = it.id) }

    val isNearby = currentNavEntry?.let { it is SheetRoutes.NearbyTransit } ?: true
    val isNearbyNotFollowing = !viewportProvider.isFollowingPuck && isNearby

    val analytics: Analytics = koinInject()
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val stopLayerGeneratorState =
        remember(selectedStop, stopFilter) {
            StopLayerGenerator.State(selectedStopId = selectedStop?.id, stopFilter = stopFilter)
        }

    suspend fun positionViewportToStop() {
        if (selectedStop != null) {
            viewportProvider.stopCenter(selectedStop!!)
            viewModel.updateCenterButtonVisibility(
                currentLocation,
                locationDataManager,
                isSearchExpanded,
                viewportProvider,
            )
        }
    }

    suspend fun updateDisplayedRoutesBasedOnStop() {
        val globalResponse = globalResponse ?: return
        val railRouteShapes = railRouteShapes ?: return
        val stopMapData = stopMapData ?: return

        val stopFilter = stopFilter
        val filteredRoutes =
            if (stopFilter != null) {
                RouteFeaturesBuilder.filteredRouteShapesForStop(
                    stopMapData,
                    stopFilter,
                    routeCardData,
                )
            } else {
                RouteFeaturesBuilder.forRailAtStop(
                    stopMapData.routeShapes,
                    railRouteShapes.routesWithSegmentedShapes,
                    globalResponse,
                )
            }
        val newRailData =
            RouteFeaturesBuilder.generateRouteSources(filteredRoutes, globalResponse, globalMapData)
        layerManager.run {
            updateRouteSourceData(newRailData)
            addLayers(
                filteredRoutes,
                stopLayerGeneratorState,
                globalResponse,
                if (isDarkMode) ColorPalette.dark else ColorPalette.light,
            )
        }
    }

    suspend fun refreshRouteLineSource() {
        val routeData = railRouteSourceData ?: return
        layerManager.run {
            updateRouteSourceData(routeData)
            addLayers(
                railRouteShapes ?: return@run,
                stopLayerGeneratorState,
                globalResponse ?: return@run,
                if (isDarkMode) ColorPalette.dark else ColorPalette.light,
            )
        }
    }

    suspend fun refreshStopSource() {
        val sourceData = stopSourceData ?: return
        layerManager.run { updateStopSourceData(sourceData) }
    }

    suspend fun handleNearbyNavRestoration() {
        if (
            (previousNavEntry is SheetRoutes.NearbyTransit ||
                previousNavEntry is SheetRoutes.Favorites) &&
                currentNavEntry is SheetRoutes.StopDetails
        ) {
            viewportProvider.saveNearbyTransitViewport()
        } else if (
            previousNavEntry is SheetRoutes.StopDetails &&
                (currentNavEntry is SheetRoutes.NearbyTransit ||
                    currentNavEntry is SheetRoutes.Favorites)
        ) {
            refreshRouteLineSource()
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    suspend fun handleNavChange() {
        handleNearbyNavRestoration()
        val stopDetails =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails -> currentNavEntry
                else -> null
            }
        if (stopDetails == null) {
            viewModel.setSelectedStop(null)
            viewModel.setStopFilter(null)
            return
        }
        viewModel.setSelectedStop(globalResponse?.getStop(stopDetails.stopId))
        viewModel.setStopFilter(stopDetails.stopFilter)
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
                addLayers(
                    railRouteShapes ?: return@run,
                    stopLayerGeneratorState,
                    globalResponse ?: return@run,
                    if (isDarkMode) ColorPalette.dark else ColorPalette.light,
                )
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
                contentScale = ContentScale.FillWidth,
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
                        alignment = Alignment.BottomEnd,
                    )
                },
                mapViewportState = viewportProvider.getViewportImmediate(),
                mapState = mapState,
                style = {
                    MapStyle(
                        style =
                            if (isDarkMode) appVariant.darkMapStyle else appVariant.lightMapStyle
                    )
                },
            ) {
                LaunchedEffect(now) { viewModel.refreshGlobalMapData(now) }
                LaunchedEffect(currentNavEntry) { handleNavChange() }
                LaunchedEffect(railRouteShapes, globalResponse, globalMapData) {
                    viewModel.refreshRouteLineData(globalMapData)
                }
                LaunchedEffect(railRouteSourceData) {
                    refreshRouteLineSource()
                    viewModel.refreshStopFeatures(globalMapData)
                }
                LaunchedEffect(selectedStop) { positionViewportToStop() }
                LaunchedEffect(stopSourceData) { refreshStopSource() }
                LaunchedEffect(selectedVehicle) {
                    if (
                        selectedVehicle != null && selectedVehicle.id != previousSelectedVehicleId
                    ) {
                        viewportProvider.vehicleOverview(
                            selectedVehicle,
                            selectedStop,
                            density.density,
                        )
                    }
                }
                LaunchedEffect(stopLayerGeneratorState) {
                    layerManager.run {
                        addLayers(
                            railRouteShapes ?: return@run,
                            stopLayerGeneratorState,
                            globalResponse ?: return@run,
                            if (isDarkMode) ColorPalette.dark else ColorPalette.light,
                        )
                    }
                }

                LaunchedEffect(stopMapData) { updateDisplayedRoutesBasedOnStop() }
                LaunchedEffect(currentNavEntry) { updateDisplayedRoutesBasedOnStop() }
                LaunchedEffect(
                    locationDataManager.hasPermission,
                    currentLocation,
                    viewportProvider.isAnimating,
                    viewportProvider.isFollowingPuck,
                    selectedVehicle,
                    isSearchExpanded,
                    viewportProvider.isVehicleOverview,
                ) {
                    if (viewportProvider.isAnimating) {
                        viewModel.hideCenterButtons()
                    } else {
                        viewModel.updateCenterButtonVisibility(
                            currentLocation,
                            locationDataManager,
                            isSearchExpanded,
                            viewportProvider,
                        )
                    }
                }
                val locationProvider = remember { PassthroughLocationProvider() }

                MapEffect(true) { map ->
                    map.mapboxMap.addOnMapClickListener { point ->
                        map.getStopIdAt(point) {
                            analytics.tappedOnStop(it)
                            handleStopNavigation(it)
                        }
                        false
                    }
                    map.mapboxMap.setBounds(
                        CameraBoundsOptions.Builder().maxZoom(18.0).minZoom(6.0).build()
                    )
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
                            if (viewportProvider.isFollowingPuck) {
                                viewportProvider.follow()
                            }
                        }
                    }
                }

                MapEffect(locationDataManager.hasPermission) { map ->
                    if (locationDataManager.hasPermission && viewportProvider.isDefault()) {

                        viewportProvider.follow(0)
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
                        Crosshairs(PaddingValues(0.dp))
                    }
                }

                for (vehicle in allVehicles) {
                    val route = globalResponse?.getRoute(vehicle.routeId) ?: continue
                    val isSelected = vehicle.id == selectedVehicle?.id
                    ViewAnnotation(
                        options =
                            viewAnnotationOptions {
                                priority(if (isSelected) 1 else 0)
                                geometry(Point.fromLngLat(vehicle.longitude, vehicle.latitude))
                                annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                                allowOverlap(true)
                                allowOverlapWithPuck(true)
                                ignoreCameraPadding(true)
                                visible(
                                    zoomLevel >= StopLayerGenerator.stopZoomThreshold || isSelected
                                )
                            }
                    ) {
                        VehiclePuck(
                            vehicle = vehicle,
                            route = route,
                            selected = isSelected,
                            onClick =
                                if (vehicle.tripId != null) {
                                    { handleVehicleTap(vehicle) }
                                } else {
                                    null
                                },
                        )
                    }
                }
            }

            if (
                !locationDataManager.hasPermission &&
                    isNearby &&
                    currentLocation == null &&
                    !viewportProvider.isFollowingPuck
            ) {
                LocationAuthButton(
                    locationDataManager,
                    modifier =
                        Modifier.align(Alignment.TopCenter).padding(top = 85.dp).statusBarsPadding(),
                )
            }

            val recenterContainerModifier =
                if (isNearby)
                    Modifier.align(Alignment.TopEnd).padding(top = 85.dp).statusBarsPadding()
                else Modifier.align(Alignment.TopEnd).padding(top = 16.dp).statusBarsPadding()

            Column(recenterContainerModifier, Arrangement.spacedBy(16.dp)) {
                if (showRecenterButton) {
                    RecenterButton(
                        Icons.Default.LocationOn,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = { coroutineScope.launch { viewportProvider.follow() } },
                    )
                }

                if (showTripCenterButton) {
                    if (selectedVehicle != null) {
                        val routeType = globalResponse?.getRoute(selectedVehicle.routeId)?.type
                        if (routeType != null) {
                            RecenterButton(
                                routeIcon(routeType).first,
                                onClick = {
                                    coroutineScope.launch {
                                        viewportProvider.vehicleOverview(
                                            selectedVehicle,
                                            selectedStop,
                                            density.density,
                                        )
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
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
                Crosshairs(sheetPadding = sheetPadding)
            }
        }
    }
}
