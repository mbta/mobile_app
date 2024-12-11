package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mapbox.maps.MapboxExperimental
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.DragHandle
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.map.IMapViewModel
import com.mbta.tid.mbta_app.android.map.MapViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitTabViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.android.search.SearchBarOverlay
import com.mbta.tid.mbta_app.android.state.RouteDirection
import com.mbta.tid.mbta_app.android.state.subscribeToVehicles
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
data class NearbyTransit(
    val alertData: AlertsStreamDataResponse?,
    val globalResponse: GlobalResponse?,
    val lastNearbyTransitLocationState: MutableState<Position?>,
    val nearbyTransitSelectingLocationState: MutableState<Boolean>,
    val scaffoldState: BottomSheetScaffoldState,
    val locationDataManager: LocationDataManager,
    val viewportProvider: ViewportProvider,
) {
    var lastNearbyTransitLocation by lastNearbyTransitLocationState
    var nearbyTransitSelectingLocation by nearbyTransitSelectingLocationState
}

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class, FlowPreview::class)
@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    bottomBar: @Composable () -> Unit,
    mapViewModel: IMapViewModel = viewModel(factory = MapViewModel.Factory())
) {
    val navController = rememberNavController()
    val currentNavEntry: NavBackStackEntry? by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    var stopDetailsDepartures by remember { mutableStateOf<StopDetailsDepartures?>(null) }
    val viewModel: NearbyTransitTabViewModel = viewModel()
    val stopDetailsFilter by viewModel.stopDetailsFilter.collectAsState()
    val vehiclesRouteDirection by viewModel.vehiclesRouteDirection.collectAsState()
    var vehiclesData: List<Vehicle> = subscribeToVehicles(routeDirection = vehiclesRouteDirection)

    fun handleStopNavigation(stopId: String) {
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun handleRouteChange(route: SheetRoutes?) {
        vehiclesData = emptyList()
        if (route is SheetRoutes.StopDetails) {

            val routeId = stopDetailsFilter?.routeId
            val directionId = stopDetailsFilter?.directionId

            vehiclesRouteDirection
            if (routeId != null && directionId != null) {
                viewModel.setVehiclesRouteDirection(RouteDirection(routeId, directionId))
                return
            }
        }

        viewModel.setVehiclesRouteDirection(null)
    }

    fun updateStopFilter(filter: StopDetailsFilter?) {
        viewModel.setStopDetailsFilter(filter)
    }

    LaunchedEffect(mapViewModel.lastMapboxErrorTimestamp.collectAsState(initial = null).value) {
        mapViewModel.loadConfig()
    }
    LaunchedEffect(nearbyTransit.alertData) { mapViewModel.setAlertsData(nearbyTransit.alertData) }

    LaunchedEffect(nearbyTransit.globalResponse) {
        mapViewModel.setGlobalResponse(nearbyTransit.globalResponse)
    }

    SearchBarOverlay(::handleStopNavigation, currentNavEntry) {
        Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
            BottomSheetScaffold(
                sheetDragHandle = { DragHandle() },
                sheetContent = {
                    var sheetHeight by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current
                    Box(
                        modifier =
                            Modifier.onGloballyPositioned {
                                    // https://issuetracker.google.com/issues/287390075#comment7
                                    sheetHeight =
                                        with(density) { it.boundsInWindow().height.toDp() }
                                }
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                    ) {
                        NavHost(
                            navController,
                            startDestination = SheetRoutes.NearbyTransit,
                            modifier =
                                Modifier.height(sheetHeight)
                                    .padding(outerSheetPadding)
                                    .background(MaterialTheme.colorScheme.surface)
                        ) {
                            composable<SheetRoutes.StopDetails> { backStackEntry ->
                                val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()
                                val stop = nearbyTransit.globalResponse?.stops?.get(navRoute.stopId)


                                fun updateStopDepartures(departures: StopDetailsDepartures?) {
                                    stopDetailsDepartures = departures
                                    if (departures != null && stopDetailsFilter == null) {
                                        updateStopFilter(departures.autoStopFilter())
                                    }
                                    }

                            LaunchedEffect(navRoute) {
                                if (navBarVisible) {
                                    hideNavBar()
                                }

                                updateStopFilter(
                                    if (
                                        navRoute.filterRouteId != null &&
                                            navRoute.filterDirectionId != null
                                    )
                                        StopDetailsFilter(
                                            navRoute.filterRouteId,
                                            navRoute.filterDirectionId
                                        )
                                    else null
                                )
                            }

                            DisposableEffect(navRoute, stopDetailsFilter) {
                                handleRouteChange(navRoute)

                                onDispose { handleRouteChange(null) }
                            }

                            if (stop != null) {
                                StopDetailsPage(
                                    modifier = modifier,
                                    stop,
                                    stopDetailsFilter,
                                    nearbyTransit.alertData,
                                    onClose = { navController.popBackStack() },
                                    updateStopFilter = ::updateStopFilter,
                                    updateDepartures = ::updateStopDepartures
                                )
                            }
                        }
                            composable<SheetRoutes.NearbyTransit> {
                                LaunchedEffect(true) {
                                    if (!navBarVisible) {
                                        showNavBar()
                                    }

                                    updateStopFilter(null)
                                }

                                var targetLocation by remember { mutableStateOf<Position?>(null) }
                                LaunchedEffect(nearbyTransit.viewportProvider) {
                                    nearbyTransit.viewportProvider.cameraStateFlow
                                        .debounce(0.5.seconds)
                                        .collect {
                                            // since this LaunchedEffect is cancelled when not on
                                            // the
                                            // nearby transit page, we don't need to check
                                            targetLocation = it.center.toPosition()
                                        }
                                }
                                LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
                                    if (nearbyTransit.viewportProvider.isManuallyCentering) {
                                        // TODO reset view model
                                        targetLocation = null
                                    }
                                }
                                LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
                                    if (nearbyTransit.viewportProvider.isFollowingPuck) {
                                        // TODO reset view model
                                        targetLocation = null
                                    }
                                }

                                NearbyTransitView(
                                    alertData = nearbyTransit.alertData,
                                    globalResponse = nearbyTransit.globalResponse,
                                    targetLocation = targetLocation,
                                    setLastLocation = {
                                        nearbyTransit.lastNearbyTransitLocation = it
                                    },
                                    setSelectingLocation = {
                                        nearbyTransit.nearbyTransitSelectingLocation = it
                                    },
                                    onOpenStopDetails = { stopId, filter ->
                                        navController.navigate(
                                            SheetRoutes.StopDetails(
                                                stopId,
                                                filter?.routeId,
                                                filter?.directionId
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                scaffoldState = nearbyTransit.scaffoldState,
                sheetPeekHeight = 422.dp,
            ) { sheetPadding ->
                HomeMapView(
                    Modifier.padding(sheetPadding),
                    lastNearbyTransitLocation = nearbyTransit.lastNearbyTransitLocation,
                    nearbyTransitSelectingLocationState =
                        nearbyTransit.nearbyTransitSelectingLocationState,
                    locationDataManager = nearbyTransit.locationDataManager,
                    viewportProvider = nearbyTransit.viewportProvider,
                    currentNavEntry = currentNavEntry,
                    handleStopNavigation = ::handleStopNavigation,
                    vehiclesData = vehiclesData,
                    stopDetailsDepartures = stopDetailsDepartures,
                    stopDetailsFilter = stopDetailsFilter,
                    viewModel = mapViewModel
                )
            }
        }
    }
}
