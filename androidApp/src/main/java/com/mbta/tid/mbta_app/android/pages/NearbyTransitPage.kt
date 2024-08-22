package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val mapViewportState: MapViewportState
)

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun NearbyTransitPage(nearbyTransit: NearbyTransit, bottomBar: @Composable () -> Unit) {
    val navController = rememberNavController()
    val currentNavEntry: NavBackStackEntry? by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    fun handleStopNavigation(stopId: String) {
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        BottomSheetScaffold(
            sheetDragHandle = {
                Column(
                    modifier =
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BottomSheetDefaults.DragHandle()
                }
            },
            sheetContent = {
                NavHost(
                    navController,
                    startDestination = SheetRoutes.NearbyTransit,
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(outerSheetPadding)
                            .background(MaterialTheme.colorScheme.surface)
                ) {
                    composable<SheetRoutes.NearbyTransit> {
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
                    composable<SheetRoutes.StopDetails> { backStackEntry ->
                        val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()
                        val stop = nearbyTransit.globalResponse?.stops?.get(navRoute.stopId)
                        val filterState = remember {
                            val filter =
                                if (
                                    navRoute.filterRouteId != null &&
                                        navRoute.filterDirectionId != null
                                )
                                    StopDetailsFilter(
                                        navRoute.filterRouteId,
                                        navRoute.filterDirectionId
                                    )
                                else null
                            mutableStateOf(filter)
                        }
                        if (stop != null) {
                            StopDetailsPage(
                                stop,
                                filterState,
                                nearbyTransit.alertData,
                                onClose = { navController.popBackStack() }
                            )
                        }
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
                handleStopNavigation = ::handleStopNavigation,
            )
        }
    }
}
