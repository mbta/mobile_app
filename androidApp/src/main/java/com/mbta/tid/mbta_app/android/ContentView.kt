package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.android.component.BottomNavBar
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.location.rememberLocationDataManager
import com.mbta.tid.mbta_app.android.pages.MorePage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.NearbyTransitPage
import com.mbta.tid.mbta_app.android.phoenix.PhoenixSocketWrapper
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.subscribeToAlerts
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixSocket
import io.github.dellisd.spatialk.geojson.Position
import org.koin.compose.koinInject

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class)
@Composable
fun ContentView(
    socket: PhoenixSocket = koinInject(),
) {
    val navController = rememberNavController()
    var alertData: AlertsStreamDataResponse? = subscribeToAlerts()
    val globalResponse = getGlobalData()
    val locationDataManager = rememberLocationDataManager()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(ViewportProvider.Companion.Defaults.center)
            zoom(ViewportProvider.Companion.Defaults.zoom)
            pitch(0.0)
            bearing(0.0)
            transitionToFollowPuckState()
        }
    }
    val viewportProvider = remember { ViewportProvider(mapViewportState) }
    val lastNearbyTransitLocation by remember { mutableStateOf<Position?>(null) }
    val scaffoldState =
        rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState())
    var navBarVisible by remember { mutableStateOf(true) }

    DisposableEffect(null) {
        socket.attach()
        (socket as? PhoenixSocketWrapper)?.attachLogging()
        onDispose { socket.detach() }
    }
    val sheetModifier = Modifier.fillMaxSize().background(colorResource(id = R.color.fill1))
    NavHost(navController = navController, startDestination = Routes.NearbyTransit) {
        composable<Routes.NearbyTransit> {
            NearbyTransitPage(
                modifier = sheetModifier,
                NearbyTransit(
                    alertData = alertData,
                    globalResponse = globalResponse,
                    lastNearbyTransitLocation = lastNearbyTransitLocation,
                    scaffoldState = scaffoldState,
                    locationDataManager = locationDataManager,
                    viewportProvider = viewportProvider,
                ),
                navBarVisible = navBarVisible,
                showNavBar = { navBarVisible = true },
                hideNavBar = { navBarVisible = false },
                bottomBar = {
                    if (navBarVisible) {
                        BottomNavBar(navController = navController)
                    }
                }
            )
        }
        composable<Routes.More> {
            MorePage(bottomBar = { BottomNavBar(navController = navController) })
        }
    }
}
