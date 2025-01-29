package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripAndFormat
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun StopDetailsFilteredDeparturesView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    patternsByStop: PatternsByStop,
    tileData: List<TripAndFormat>,
    noPredictionsStatus: RealtimePatterns.NoTripsFormat?,
    elevatorAlerts: List<Alert>,
    global: GlobalResponse?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit
) {
    val expectedDirection = stopFilter.directionId
    val showElevatorAccessibility by viewModel.showElevatorAccessibility.collectAsState()
    val hideMaps by viewModel.hideMaps.collectAsState()

    val alerts: List<Alert> =
        if (global != null) {
            patternsByStop.alertsHereFor(directionId = expectedDirection, global = global)
        } else {
            emptyList()
        }

    val selectedTripIsCancelled: Boolean =
        tripFilter?.let { patternsByStop.tripIsCancelled(tripFilter.tripId) } ?: false

    val routeHex: String = patternsByStop.line?.color ?: patternsByStop.representativeRoute.color
    val routeColor: Color = Color.fromHex(routeHex)
    val routeType: RouteType = patternsByStop.representativeRoute.type

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        StopDetailsFilteredHeader(
            patternsByStop.representativeRoute,
            patternsByStop.line,
            patternsByStop.stop,
            pinned = pinnedRoutes.contains(patternsByStop.routeIdentifier),
            onPin = { togglePinnedRoute(patternsByStop.routeIdentifier) },
            onClose = onClose
        )

        ErrorBanner(errorBannerViewModel)

        Box(Modifier.fillMaxSize().background(routeColor)) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DirectionPicker(patternsByStop, stopFilter, updateStopFilter)

                if (showElevatorAccessibility && elevatorAlerts.isNotEmpty()) {
                    elevatorAlerts.map {
                        Column(
                            Modifier.background(
                                    colorResource(R.color.fill3),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.dp, Color.Unspecified, shape = RoundedCornerShape(8.dp))
                                .clickable(
                                    onClickLabel = stringResource(R.string.displays_more_info)
                                ) {
                                    openAlertDetails(
                                        ModalRoutes.AlertDetails(it.id, null, null, stopId)
                                    )
                                }
                        ) {
                            StopDetailsAlertHeader(it, Color.Unspecified, showInfoIcon = true)
                        }
                    }
                }

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
                            if (index < alerts.size - 1 || tileData.isNotEmpty()) {
                                HorizontalDivider(Modifier.background(colorResource(R.color.halo)))
                            }
                        }
                    }

                    for ((index, row) in tileData.withIndex()) {
                        val modifier =
                            if (index == 0 || index == tileData.size - 1)
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

                if (noPredictionsStatus != null) {
                    StopDetailsNoTripCard(
                        status = RealtimePatterns.NoTripsFormat.ServiceEndedToday,
                        accentColor = routeColor,
                        routeType = routeType,
                        hideMaps = hideMaps
                    )
                } else if (selectedTripIsCancelled) {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp).semantics { focused = true }
                    ) {
                        StopDetailsIconCard(
                            routeColor,
                            details = { Text(stringResource(R.string.trip_cancelled_details)) },
                            header = { modifier ->
                                Text(stringResource(R.string.trip_cancelled), modifier = modifier)
                            },
                            icon = { modifier ->
                                Icon(
                                    painter = routeSlashIcon(routeType = routeType),
                                    contentDescription = null,
                                    modifier = modifier.testTag("route_slash_icon"),
                                )
                            }
                        )
                    }
                } else {
                    TripDetailsView(
                        tripFilter = tripFilter,
                        stopId = stopId,
                        stopDetailsVM = viewModel,
                        setMapSelectedVehicle = setMapSelectedVehicle,
                        now = now
                    )
                }
            }
        }
    }
}
