package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mapbox.maps.MapboxExperimental
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@ExperimentalMaterial3Api
@MapboxExperimental
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    stop: Stop,
    filter: StopDetailsFilter?,
    alertData: AlertsStreamDataResponse?,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateDepartures: (StopDetailsDepartures?) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData("StopDetailsPage.getGlobalData")

    val predictionsVM =
        subscribeToPredictions(
            stopIds = listOf(stop.id),
            errorBannerViewModel = errorBannerViewModel
        )
    val predictionsResponse by predictionsVM.predictionsFlow.collectAsState(initial = null)

    val now = timer(updateInterval = 5.seconds)

    val schedulesResponse = getSchedule(stopIds = listOf(stop.id), "StopDetailsPage.getSchedule")

    val (pinnedRoutes, togglePinnedRoute) = managePinnedRoutes()

    val departures =
        rememberSuspend(
            stop,
            globalResponse,
            schedulesResponse,
            predictionsResponse,
            alertData,
            pinnedRoutes,
            now
        ) {
            withContext(Dispatchers.Default) {
                if (globalResponse != null) {
                    StopDetailsDepartures.fromData(
                        stop,
                        globalResponse,
                        schedulesResponse,
                        predictionsResponse,
                        alertData,
                        pinnedRoutes.orEmpty(),
                        now,
                        useTripHeadsigns = false,
                    )
                } else null
            }
        }

    LaunchedEffect(departures) { updateDepartures(departures) }

    StopDetailsView(
        modifier,
        stop,
        filter,
        departures,
        pinnedRoutes.orEmpty(),
        togglePinnedRoute,
        onClose,
        updateStopFilter,
        errorBannerViewModel
    )
}
