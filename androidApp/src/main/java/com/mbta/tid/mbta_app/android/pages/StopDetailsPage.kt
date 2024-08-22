package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.component.DragHandle
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.android.util.getGlobalData
import com.mbta.tid.mbta_app.android.util.getSchedule
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@Composable
@ExperimentalMaterial3Api
@MapboxExperimental
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    stop: Stop,
    filterState: MutableState<StopDetailsFilter?>,
    alertData: AlertsStreamDataResponse?,
    scaffoldState: BottomSheetScaffoldState,
    mapViewportState: MapViewportState,
    lastNearbyTransitLocation: Position?,
    onClose: () -> Unit
) {
    val globalResponse = getGlobalData()

    val predictionsResponse = subscribeToPredictions(stopIds = listOf(stop.id))

    val now = timer(updateInterval = 5.seconds)

    val schedulesResponse = getSchedule(stopIds = listOf(stop.id), now)

    val (pinnedRoutes, togglePinnedRoute) = managePinnedRoutes()

    val departures =
        remember(
            stop,
            globalResponse,
            schedulesResponse,
            predictionsResponse,
            alertData,
            pinnedRoutes,
            now
        ) {
            if (globalResponse != null) {
                StopDetailsDepartures(
                    stop,
                    globalResponse,
                    schedulesResponse,
                    predictionsResponse,
                    alertData,
                    pinnedRoutes.orEmpty(),
                    now
                )
            } else null
        }
    BottomSheetScaffold(
        sheetDragHandle = { DragHandle() },
        sheetContent = {
            StopDetailsView(
                modifier,
                stop,
                filterState,
                departures,
                pinnedRoutes.orEmpty(),
                togglePinnedRoute,
                onClose
            )
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 422.dp,
    ) { sheetPadding ->
        HomeMapView(
            Modifier.padding(sheetPadding),
            mapViewportState,
            globalResponse = globalResponse,
            alertsData = alertData,
            lastNearbyTransitLocation = lastNearbyTransitLocation
        )
    }
}
