package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.LineHeader
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RouteHeader
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun StopDetailsFilteredRouteView(
    stopId: String,
    viewModel: StopDetailsViewModel,
    global: GlobalResponse?,
    now: Instant,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit
) {  val departures = viewModel.stopDepartures.collectAsState().value

    if (departures != null) {
        val patternsByStop = departures.routes.find { it.routeIdentifier == stopFilter.routeId }
        val expectedDirection = stopFilter.directionId
        if (patternsByStop == null) {
            return
        }

        val data =
            departures.stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, now)

        val alerts: List<Alert> =
            if (global != null) {
                patternsByStop.alertsHereFor(directionId = expectedDirection, global = global)
            } else {
                emptyList()
            }

        val routeHex: String =
            patternsByStop.line?.color ?: patternsByStop.representativeRoute.color
        val routeColor: Color = Color.fromHex(routeHex)
        Box(Modifier.fillMaxSize().background(routeColor)) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            Column(
                Modifier.fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (patternsByStop.line != null) {
                    LineHeader(patternsByStop.line!!, patternsByStop.routes)
                } else {
                    RouteHeader(patternsByStop.representativeRoute)
                }
                DirectionPicker(patternsByStop, stopFilter, updateStopFilter)

                Spacer(modifier = Modifier.padding(8.dp))

                Column(
                    Modifier.background(colorResource(R.color.fill3), RoundedCornerShape(8.dp))
                ) {
                    for ((index, alert) in alerts.withIndex()) {
                        Column {
                            StopDetailsAlertHeader(
                                alert,
                                routeColor,
                                modifier =
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.displays_more_info)
                                ) {
                                    openAlertDetails(
                                        ModalRoutes.AlertDetails(
                                            alertId = alert.id,
                                            lineId = patternsByStop.line?.id,
                                            routeIds = patternsByStop.routes.map { it.id },
                                            stopId = patternsByStop.stop.id
                                        )
                                    )
                                }
                            )
                            if (index < alerts.size - 1 || data.isNotEmpty()) {
                                HorizontalDivider(Modifier.background(colorResource(R.color.halo)))
                            }
                        }
                    }
                    for ((index, row) in data.withIndex()) {
                        val modifier =
                            if (index == 0 || index == data.size - 1)
                                Modifier.background(
                                    colorResource(R.color.fill3),
                                    RoundedCornerShape(8.dp)
                                )
                            else Modifier.background(colorResource(R.color.fill3))

                        val route =
                            patternsByStop.routes.first { it.id == row.upcoming.trip.routeId }

                        Column(modifier.border(1.dp, colorResource(R.color.halo))) {
                            HeadsignRowView(
                                row.upcoming.trip.headsign,
                                RealtimePatterns.Format.Some(listOf(row.formatted), null),
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    .clickable(
                                        onClickLabel = null,
                                        onClick = {
                                            updateTripFilter(
                                                TripDetailsFilter(
                                                    row.upcoming.trip.id,
                                                    row.upcoming.vehicle?.id,
                                                    row.upcoming.stopSequence
                                                )
                                            )
                                        }
                                    ),
                                pillDecoration =
                                if (patternsByStop.line != null)
                                    PillDecoration.OnRow(route = route)
                                else null
                            )
                        }
                    }
                }

                TripDetailsView(
                    tripFilter = tripFilter,
                    stopId = stopId,
                    stopDetailsVM = viewModel,
                    now = now
                )
            }
        }
    }
}
