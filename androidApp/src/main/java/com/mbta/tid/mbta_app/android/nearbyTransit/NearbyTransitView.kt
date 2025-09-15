package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NearbyTransitView(
    alertData: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
    targetLocation: Position?,
    setLastLocation: (Position) -> Unit,
    setIsTargeting: (Boolean) -> Unit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    noNearbyStopsView: @Composable () -> Unit,
    nearbyVM: NearbyTransitViewModel = koinViewModel(),
    errorBannerViewModel: IErrorBannerViewModel,
) {
    LaunchedEffect(targetLocation, globalResponse) {
        if (globalResponse != null && targetLocation != null) {
            nearbyVM.getNearby(globalResponse, targetLocation, setLastLocation, setIsTargeting)
        }
    }
    val now by timer(updateInterval = 5.seconds)
    val stopIds = nearbyVM.nearbyStopIds
    val schedules = getSchedule(stopIds, "NearbyTransitView.getSchedule")
    val predictionsVM = subscribeToPredictions(stopIds, errorBannerViewModel = errorBannerViewModel)
    val predictions by predictionsVM.predictionsFlow.collectAsState(initial = null)

    val analytics: Analytics = koinInject()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(targetLocation == null) {
        if (targetLocation == null) {
            predictionsVM.reset()
        }
    }
    val (favorites) = manageFavorites()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(title = stringResource(R.string.nearby_transit))
        ErrorBanner(errorBannerViewModel)
        LaunchedEffect(
            stopIds,
            globalResponse,
            targetLocation,
            schedules,
            predictions,
            alertData,
            now,
        ) {
            nearbyVM.loadRouteCardData(
                globalResponse,
                targetLocation,
                schedules,
                predictions,
                alertData,
                now,
            )
        }

        val routeCardData = nearbyVM.routeCardData

        RouteCardList(
            routeCardData = routeCardData,
            emptyView = {
                noNearbyStopsView()
                Spacer(Modifier.weight(1f))
            },
            global = globalResponse,
            now = now,
            isFavorite = { rsd -> (favorites ?: emptySet()).contains(rsd) },
            onOpenStopDetails = onOpenStopDetails,
        )
    }
}
