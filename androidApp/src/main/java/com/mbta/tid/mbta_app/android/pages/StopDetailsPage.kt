package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mapbox.maps.MapboxExperimental
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
import kotlin.time.Duration.Companion.seconds

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

    StopDetailsView(
        modifier,
        stop,
        filter,
        departures,
        pinnedRoutes.orEmpty(),
        togglePinnedRoute,
        onClose,
        updateStopFilter,
    )
}
