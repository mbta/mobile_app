package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
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
fun StopDetailsPage(
    stop: Stop,
    filterState: MutableState<StopDetailsFilter?>,
    alertData: AlertsStreamDataResponse?,
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

    StopDetailsView(
        stop,
        filterState,
        departures,
        pinnedRoutes.orEmpty(),
        togglePinnedRoute,
        onClose
    )
}
