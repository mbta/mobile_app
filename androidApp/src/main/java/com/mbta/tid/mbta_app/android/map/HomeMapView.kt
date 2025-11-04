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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
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
import com.mapbox.maps.plugin.LocationPuck2D
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
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.getStopIdAt
import com.mbta.tid.mbta_app.android.util.plus
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.viewModel.IMapViewModel
import com.mbta.tid.mbta_app.viewModel.MapViewModel
import com.mbta.tid.mbta_app.viewModel.MapViewModel.State
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun HomeMapView(
    sheetPadding: PaddingValues,
    lastLoadedLocation: Position?,
    isTargetingState: MutableState<Boolean>,
    locationDataManager: LocationDataManager,
    viewportProvider: IViewportProvider,
    currentNavEntry: SheetRoutes?,
    handleStopNavigation: (String) -> Unit,
    handleVehicleTap: (Vehicle) -> Unit,
    handleBack: (() -> Unit)?,
    vehiclesData: List<Vehicle>,
    viewModel: IMapViewModel,
    mapboxConfigManager: IMapboxConfigManager = koinInject(),
) {
    val globalData = getGlobalData("HomeMapView")
    val state by viewModel.models.collectAsState()
    var isTargeting by isTargetingState

    val configLoadAttempted by
        mapboxConfigManager.configLoadAttempted.collectAsState(initial = false)

    val currentLocation by locationDataManager.currentLocation.collectAsState(initial = null)
    val isDarkMode = isSystemInDarkTheme()

    val allowTargeting = currentNavEntry?.allowTargeting ?: true

    var selectedVehicle by remember {
        mutableStateOf<Vehicle?>((state as? State.TripSelected)?.vehicle)
    }

    val showTripRecenterButton =
        when (state) {
            is State.StopSelected,
            is State.Overview -> false
            is State.TripSelected -> !viewportProvider.isVehicleOverview
        }

    val analytics: Analytics = koinInject()
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val cameraZoomFlow =
        remember(viewportProvider.cameraStateFlow) {
            viewportProvider.cameraStateFlow.map { it.zoom }
        }
    val zoomLevel by
        cameraZoomFlow.collectAsState(initial = ViewportProvider.Companion.Defaults.zoom)

    val showCurrentLocation = currentNavEntry?.showCurrentLocation ?: true
    val pulsingRingColor: Int = colorResource(R.color.key_inverse).toArgb()
    val accuracyRingColor: Int = colorResource(R.color.deemphasized).copy(alpha = 0.1F).toArgb()
    val accuracyRingBorderColor: Int = colorResource(R.color.halo).toArgb()
    val mapState = rememberMapState()
    mapState.gesturesSettings = GesturesSettings {
        rotateEnabled = false
        pitchEnabled = false
    }
    LaunchedEffect(Unit) { mapState.styleLoadedEvents.collect { viewModel.mapStyleLoaded() } }
    LaunchedEffect(Unit) {
        mapState.cameraChangedEvents.collect { viewportProvider.updateCameraState(it.cameraState) }
    }
    LaunchedEffect(viewportProvider, sheetPadding) {
        viewportProvider.setSheetPadding(sheetPadding, density, layoutDirection)
    }
    LaunchedEffect(isDarkMode) { viewModel.colorPaletteChanged(isDarkMode) }
    LaunchedEffect(density) { viewModel.densityChanged(density.density) }
    LaunchedEffect(currentNavEntry) { viewModel.navChanged(currentNavEntry) }
    LaunchedEffect((state as? State.TripSelected)?.vehicle) {
        val vehicle = (state as? State.TripSelected)?.vehicle
        val skipSettingVehicle =
            selectedVehicle?.id == currentNavEntry?.vehicleId && vehicle == null
        if (skipSettingVehicle) return@LaunchedEffect
        selectedVehicle = vehicle
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
                val locationProvider = remember { PassthroughLocationProvider() }

                MapEffect { map ->
                    val layerManager = MapLayerManager(map.mapboxMap, context)
                    layerManager.loadImages()
                    layerManager.setUpAnchorLayers()
                    map.mapboxMap.addOnMapClickListener { point ->
                        map.getStopIdAt(point) {
                            handleStopNavigation(it)
                            analytics.tappedOnStop(it)
                        }
                        return@addOnMapClickListener false
                    }
                    map.mapboxMap.setBounds(
                        CameraBoundsOptions.Builder().maxZoom(18.0).minZoom(6.0).build()
                    )
                    map.location.setLocationProvider(locationProvider)
                    map.location.updateSettings {
                        enabled = true
                        puckBearingEnabled = false
                        pulsingEnabled = true
                        pulsingColor = pulsingRingColor
                        pulsingMaxRadius = 24F
                        showAccuracyRing = true
                        this.accuracyRingColor = accuracyRingColor
                        this.accuracyRingBorderColor = accuracyRingBorderColor
                        layerBelow = MapLayerManager.puckAnchorLayerId
                    }

                    viewModel.layerManagerInitialized(layerManager)
                }

                MapEffect(showCurrentLocation) { map ->
                    map.location.updateSettings {
                        locationPuck =
                            if (showCurrentLocation) {
                                createDefault2DPuck(withBearing = false)
                            } else {
                                LocationPuck2D(opacity = 0f)
                            }
                    }
                }

                LaunchedEffect(locationDataManager) {
                    locationDataManager.currentLocation.collect { location ->
                        if (location != null) {
                            locationProvider.sendLocation(
                                Point.fromLngLat(location.longitude, location.latitude)
                            )
                            if (viewportProvider.isFollowingPuck) {
                                viewModel.recenter(MapViewModel.Event.RecenterType.CurrentLocation)
                            }
                        }
                    }
                }

                MapEffect(locationDataManager.hasPermission) { map ->
                    viewModel.locationPermissionsChanged(locationDataManager.hasPermission)
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

                if (
                    !viewportProvider.isFollowingPuck &&
                        allowTargeting &&
                        lastLoadedLocation != null &&
                        !isTargeting
                ) {
                    ViewAnnotation(
                        options =
                            ViewAnnotationOptions.Builder()
                                .geometry(lastLoadedLocation.toPoint())
                                .annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                                .build()
                    ) {
                        Crosshairs(PaddingValues(0.dp))
                    }
                }

                // Maintain a mutable state list of the vehicles displayed on the map, and modify
                // that in place when there are changes to the vehicle data, to prevent issues with
                // compose forgetting the vehicle identity when the list changes, which results in
                // all the vehicles on the map flickering in and out.
                val allVehicles = remember { mutableStateListOf<Vehicle>() }
                LaunchedEffect(vehiclesData, selectedVehicle) {
                    val updated = mutableSetOf<String>()
                    val updateVehicles =
                        (selectedVehicle?.let { mapOf(it.id to it) } ?: emptyMap()).plus(
                            vehiclesData.associateBy { it.id }
                        )

                    // Remove any vehicles that don't exist in the update
                    allVehicles.removeAll { !updateVehicles.containsKey(it.id) }
                    // Replace all existing vehicles in the list with their updated vehicle objects
                    allVehicles.replaceAll {
                        updateVehicles[it.id]?.let { updateVehicle ->
                            updated.add(updateVehicle.id)
                            updateVehicle
                        } ?: it
                    }
                    // Add any vehicles that exist in the update which weren't already in the list
                    allVehicles.addAll(updateVehicles.filter { !updated.contains(it.key) }.values)
                }

                for (vehicle in allVehicles) {
                    val route = globalData?.getRoute(vehicle.routeId) ?: continue
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
                            onClick = { handleVehicleTap(vehicle) },
                        )
                    }
                }
            }

            if (
                !locationDataManager.hasPermission &&
                    allowTargeting &&
                    currentLocation == null &&
                    !viewportProvider.isFollowingPuck
            ) {
                LocationAuthButton(
                    locationDataManager,
                    modifier =
                        Modifier.align(Alignment.TopCenter).padding(top = 85.dp).statusBarsPadding(),
                )
            }

            val buttonTopPadding = if (allowTargeting) 85.dp else 16.dp

            Column(
                Modifier.align(Alignment.TopEnd)
                    .padding(top = buttonTopPadding)
                    .statusBarsPadding(),
                Arrangement.spacedBy(16.dp),
            ) {
                if (
                    showCurrentLocation &&
                        !viewportProvider.isFollowingPuck &&
                        locationDataManager.hasPermission
                ) {
                    RecenterButton(
                        Icons.Default.LocationOn,
                        stringResource(R.string.recenter),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = { viewModel.recenter() },
                    )
                }

                if (showTripRecenterButton) {
                    selectedVehicle?.let { vehicle ->
                        globalData?.getRoute(vehicle.routeId)?.type?.let { routeType ->
                            RecenterButton(
                                routeIcon(routeType).first,
                                stringResource(
                                    R.string.recenter_map_on_vehicle,
                                    routeType.typeText(LocalContext.current, isOnly = true),
                                ),
                                onClick = {
                                    viewModel.recenter(MapViewModel.Event.RecenterType.Trip)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }

            LaunchedEffect(viewportProvider.isManuallyCentering) {
                if (
                    viewportProvider.isManuallyCentering && currentNavEntry?.allowTargeting == true
                ) {
                    isTargeting = true
                }
            }

            if (handleBack != null) {
                RecenterButton(
                    painterResource(R.drawable.fa_chevron_left),
                    stringResource(R.string.back_button_label),
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .padding(top = buttonTopPadding)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp)
                            .testTag("backButton"),
                    onClick = { handleBack() },
                )
            }

            if (isTargeting && allowTargeting) {
                Crosshairs(sheetPadding = sheetPadding)
            }
        }
    }
}
