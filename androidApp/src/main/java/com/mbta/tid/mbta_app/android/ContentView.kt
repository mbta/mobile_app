package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.android.component.BottomNavBar
import com.mbta.tid.mbta_app.android.pages.MorePage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.NearbyTransitPage
import com.mbta.tid.mbta_app.android.phoenix.PhoenixSocketWrapper
import com.mbta.tid.mbta_app.android.util.getGlobalData
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ContentView(
    alertsRepository: IAlertsRepository = koinInject(),
    socket: PhoenixSocket = koinInject(),
) {
    val navController = rememberNavController()
    var alertData: AlertsStreamDataResponse? by remember { mutableStateOf(null) }
    DisposableEffect(null) {
        val scope = CoroutineScope(Dispatchers.IO)
        val job =
            scope.launch {
                alertsRepository.connect {
                    when (it) {
                        is ApiResult.Ok -> alertData = it.data
                        is ApiResult.Error -> TODO("handle errors")
                    }
                }
            }
        onDispose {
            alertsRepository.disconnect()
            job.cancel()
        }
    }
    val globalResponse = getGlobalData()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-71.062424, 42.356395))
            zoom(13.25)
            pitch(0.0)
            bearing(0.0)
            transitionToFollowPuckState()
        }
    }
    val rawMapCenterFlow = snapshotFlow { mapViewportState.cameraState.center }
    val mapCenterFlow =
        remember(rawMapCenterFlow) {
            rawMapCenterFlow.debounce(0.25.seconds).map { it.toPosition() }
        }
    val mapCenter by
        mapCenterFlow.collectAsState(
            initial = Position(longitude = -71.062424, latitude = 42.356395)
        )
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
                    targetLocation = mapCenter,
                    mapCenter = mapCenter,
                    lastNearbyTransitLocation = lastNearbyTransitLocation,
                    scaffoldState = scaffoldState,
                    mapViewportState = mapViewportState,
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
