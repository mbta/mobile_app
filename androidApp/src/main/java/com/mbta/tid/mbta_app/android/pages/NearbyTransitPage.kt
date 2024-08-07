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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.android.fetcher.GlobalData
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalMaterial3Api::class)
data class NearbyTransit
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
constructor(
    val alertData: AlertsStreamDataResponse?,
    val globalData: GlobalData,
    val targetLocation: Position,
    val mapCenter: Position,
    var lastNearbyTransitLocation: Position?,
    val scaffoldState: BottomSheetScaffoldState,
    val mapViewportState: MapViewportState,
    val backend: Backend
)

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun NearbyTransitPage(nearbyTransit: NearbyTransit, bottomBar: @Composable () -> Unit) {
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
                NearbyTransitView(
                    Modifier.fillMaxSize()
                        .padding(outerSheetPadding)
                        .background(MaterialTheme.colorScheme.surface),
                    alertData = nearbyTransit.alertData,
                    globalData = nearbyTransit.globalData,
                    targetLocation = nearbyTransit.mapCenter,
                    setLastLocation = { nearbyTransit.lastNearbyTransitLocation = it },
                )
            },
            scaffoldState = nearbyTransit.scaffoldState,
            sheetPeekHeight = 422.dp,
        ) { sheetPadding ->
            HomeMapView(
                Modifier.padding(sheetPadding),
                nearbyTransit.mapViewportState,
                backend = nearbyTransit.backend,
                globalData = nearbyTransit.globalData,
                alertsData = nearbyTransit.alertData,
                lastNearbyTransitLocation = nearbyTransit.lastNearbyTransitLocation
            )
        }
    }
}
