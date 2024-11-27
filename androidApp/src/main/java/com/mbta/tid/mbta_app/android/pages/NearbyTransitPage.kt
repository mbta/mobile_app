package com.mbta.tid.mbta_app.android.pages

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.DragHandle
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import io.github.dellisd.spatialk.geojson.Position
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
data class NearbyTransit
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
constructor(
    val alertData: AlertsStreamDataResponse?,
    val globalResponse: GlobalResponse?,
    val targetLocation: Position,
    val mapCenter: Position,
    var lastNearbyTransitLocation: Position?,
    val scaffoldState: BottomSheetScaffoldState,
    val mapViewportState: MapViewportState,
)

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    vehiclesRepository: IVehiclesRepository = koinInject(),
    bottomBar: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val currentNavEntry: NavBackStackEntry? by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)
    var stopDetailsFilter by rememberSaveable { mutableStateOf<StopDetailsFilter?>(null) }
    var stopDetailsDepartures by rememberSaveable { mutableStateOf<StopDetailsDepartures?>(null) }
    var vehiclesData: List<Vehicle> by remember { mutableStateOf(emptyList()) }

    fun handleStopNavigation(stopId: String) {
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun handleReceiveVehicles(response: ApiResult<VehiclesStreamDataResponse>) {
        when (response) {
            is ApiResult.Ok -> {
                val vehicleResponse = response.data
                vehiclesData = vehicleResponse.vehicles.values.toList()
            }
            is ApiResult.Error -> {
                Log.e("Map", "Vehicle stream failed: ${response.message}")
                return
            }
        }
    }

    fun handleRouteChange(route: SheetRoutes?) {
        vehiclesData = emptyList()
        if (route is SheetRoutes.StopDetails) {
            val routeId = stopDetailsFilter?.routeId
            val directionId = stopDetailsFilter?.directionId
            if (routeId != null && directionId != null) {
                vehiclesRepository.connect(routeId, directionId, ::handleReceiveVehicles)
                return
            }
        }
        vehiclesRepository.disconnect()
    }

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
                                sheetHeight = with(density) { it.boundsInWindow().height.toDp() }
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

                            fun updateStopFilter(filter: StopDetailsFilter?) {
                                stopDetailsFilter = filter
                            }

                            fun updateStopDepartures(departures: StopDetailsDepartures?) {
                                stopDetailsDepartures = departures
                                if (departures != null && stopDetailsFilter == null) {
                                    stopDetailsFilter = departures.autoStopFilter()
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

                                stopDetailsFilter = null
                            }

                            NearbyTransitView(
                                alertData = nearbyTransit.alertData,
                                globalResponse = nearbyTransit.globalResponse,
                                targetLocation = nearbyTransit.mapCenter,
                                setLastLocation = { nearbyTransit.lastNearbyTransitLocation = it },
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
                nearbyTransit.mapViewportState,
                globalResponse = nearbyTransit.globalResponse,
                alertsData = nearbyTransit.alertData,
                lastNearbyTransitLocation = nearbyTransit.lastNearbyTransitLocation,
                currentNavEntry = currentNavEntry,
                handleStopNavigation = ::handleStopNavigation,
                vehiclesData = vehiclesData,
                stopDetailsDepartures = stopDetailsDepartures,
                stopDetailsFilter = stopDetailsFilter
            )
        }
    }
}
