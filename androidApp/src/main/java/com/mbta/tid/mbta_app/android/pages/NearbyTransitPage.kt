package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
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
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position

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
    val navController: NavController
)

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val currentNavEntry: NavBackStackEntry? by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)
    var stopDetailsFilter by rememberSaveable { mutableStateOf<StopDetailsFilter?>(null) }

    fun handleStopNavigation(stopId: String) {
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        BottomSheetScaffold(
            sheetDragHandle = { DragHandle() },
            sheetContent = {
                NavHost(
                    navController,
                    startDestination = SheetRoutes.NearbyTransit,
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(outerSheetPadding)
                            .background(MaterialTheme.colorScheme.surface)
                ) {
                    composable<SheetRoutes.StopDetails> { backStackEntry ->
                        if (navBarVisible) {
                            hideNavBar()
                        }

                        val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()
                        val stop = nearbyTransit.globalResponse?.stops?.get(navRoute.stopId)

                        fun updateStopFilter(filter: StopDetailsFilter?) {
                            stopDetailsFilter = filter
                        }

                        LaunchedEffect(navRoute) {
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

                        if (stop != null) {
                            StopDetailsPage(
                                modifier = modifier,
                                stop,
                                stopDetailsFilter,
                                nearbyTransit.alertData,
                                onClose = { navController.popBackStack() },
                                updateStopFilter = ::updateStopFilter
                            )
                        }
                    }
                    composable<SheetRoutes.NearbyTransit> {
                        if (!navBarVisible) {
                            showNavBar()
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
            },
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
                handleStopNavigation = ::handleStopNavigation
            )
        }
    }
}
