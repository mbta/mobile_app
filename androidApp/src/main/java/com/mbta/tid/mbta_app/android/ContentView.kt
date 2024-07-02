package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
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
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.android.fetcher.fetchGlobalData
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitPage
import com.mbta.tid.mbta_app.android.phoenix.PhoenixSocketWrapper
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ContentView(
    appVariant: AppVariant,
    alertsRepository: IAlertsRepository = koinInject(),
    socket: PhoenixSocket = koinInject(),
) {
    val backend = remember { Backend(appVariant) }
    var alertData: AlertsStreamDataResponse? by remember { mutableStateOf(null) }
    DisposableEffect(null) {
        alertsRepository.connect { alertData = it.data }
        onDispose { alertsRepository.disconnect() }
    }
    val globalData = fetchGlobalData(backend = backend)
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
    var lastNearbyTransitLocation by remember { mutableStateOf<Position?>(null) }
    val scaffoldState =
        rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState())

    DisposableEffect(null) {
        socket.attach()
        (socket as? PhoenixSocketWrapper)?.attachLogging()
        onDispose { socket.detach() }
    }

    BottomSheetScaffold(
        sheetContent = {
            NearbyTransitPage(
                Modifier.fillMaxSize(),
                alertData = alertData,
                globalData = globalData,
                targetLocation = mapCenter,
                setLastLocation = { lastNearbyTransitLocation = it },
            )
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 200.dp,
    ) { sheetPadding ->
        HomeMapView(
            Modifier.padding(sheetPadding),
            mapViewportState,
            backend = backend,
            globalData = globalData,
            alertsData = alertData,
            lastNearbyTransitLocation = lastNearbyTransitLocation
        )
    }
}
