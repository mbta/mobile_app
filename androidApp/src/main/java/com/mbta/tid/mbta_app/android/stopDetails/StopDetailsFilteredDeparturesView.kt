package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSignificance
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopDetailsFilteredDeparturesView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    patternsByStop: PatternsByStop,
    tileData: List<TileData>,
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
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    analytics: Analytics = koinInject()
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
    val pinned = pinnedRoutes.contains(patternsByStop.routeIdentifier)

    val downstreamAlerts: List<Alert> =
        if (global != null) patternsByStop.alertsDownstream(expectedDirection) else emptyList()

    val selectedTripIsCancelled: Boolean =
        tripFilter?.let { patternsByStop.tripIsCancelled(tripFilter.tripId) } ?: false

    val hasMajorAlert = alerts.any { it.significance == AlertSignificance.Major }

    val routeHex: String = patternsByStop.line?.color ?: patternsByStop.representativeRoute.color
    val routeColor: Color = Color.fromHex(routeHex)
    val routeType: RouteType = patternsByStop.representativeRoute.type
    val textHex: String =
        patternsByStop.line?.textColor ?: patternsByStop.representativeRoute.textColor
    val routeTextColor: Color = Color.fromHex(textHex)

    // keys are trip IDs
    val bringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }

    LaunchedEffect(tripFilter) {
        if (tripFilter != null) {
            bringIntoViewRequesters[tripFilter.tripId]?.bringIntoView()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        StopDetailsFilteredHeader(
            patternsByStop.representativeRoute,
            patternsByStop.line,
            patternsByStop.stop,
            pinned = pinned,
            onPin = { togglePinnedRoute(patternsByStop.routeIdentifier) },
            onClose = onClose
        )

        ErrorBanner(errorBannerViewModel, Modifier.padding(vertical = 16.dp))

        Box(Modifier.fillMaxSize().background(routeColor)) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DirectionPicker(
                    patternsByStop,
                    stopFilter,
                    updateStopFilter,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                if (!hasMajorAlert && !tileData.isEmpty()) {
                    DepartureTiles(
                        tripFilter,
                        patternsByStop,
                        tileData,
                        alerts,
                        updateTripFilter,
                        pinned,
                        analytics,
                        bringIntoViewRequesters
                    )
                }

                @Composable
                fun AlertCard(alert: Alert, spec: AlertCardSpec? = null) {
                    val spec =
                        spec
                            ?: if (alert.significance == AlertSignificance.Major) {
                                AlertCardSpec.Major
                            } else {
                                AlertCardSpec.Secondary
                            }
                    AlertCard(
                        alert,
                        spec,
                        color = routeColor,
                        textColor = routeTextColor,
                        onViewDetails = {
                            openModal(
                                ModalRoutes.AlertDetails(
                                    alertId = alert.id,
                                    lineId =
                                        if (spec == AlertCardSpec.Elevator) null
                                        else patternsByStop.line?.id,
                                    routeIds =
                                        if (spec == AlertCardSpec.Elevator) null
                                        else patternsByStop.routes.map { it.id },
                                    stopId = patternsByStop.stop.id
                                )
                            )
                        }
                    )
                }

                if (
                    alerts.isNotEmpty() ||
                        downstreamAlerts.isNotEmpty() ||
                        (showElevatorAccessibility && elevatorAlerts.isNotEmpty())
                ) {
                    Column(
                        Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        alerts.forEach { AlertCard(it) }
                        downstreamAlerts.forEach { AlertCard(it, AlertCardSpec.Downstream) }
                        if (showElevatorAccessibility) {
                            elevatorAlerts.forEach { AlertCard(it, AlertCardSpec.Elevator) }
                        }
                    }
                }

                if (hasMajorAlert) {
                    Box {}
                } else if (noPredictionsStatus != null) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 12.dp)) {
                        StopDetailsNoTripCard(
                            status = noPredictionsStatus,
                            accentColor = routeColor,
                            routeType = routeType,
                            hideMaps = hideMaps
                        )
                    }
                } else if (selectedTripIsCancelled) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)) {
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
                        openSheetRoute = openSheetRoute,
                        openModal = openModal,
                        now = now,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DepartureTiles(
    tripFilter: TripDetailsFilter?,
    patternsByStop: PatternsByStop,
    tiles: List<TileData>,
    alerts: List<Alert>,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    pinned: Boolean,
    analytics: Analytics,
    bringIntoViewRequesters: MutableMap<String, BringIntoViewRequester>
) {
    val coroutineScope = rememberCoroutineScope()
    val showTileHeadsigns =
        patternsByStop.line != null || !tiles.all { it.headsign == tiles.firstOrNull()?.headsign }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (tileData in tiles) {
            val bringIntoViewRequester =
                bringIntoViewRequesters.getOrPut(tileData.id, ::BringIntoViewRequester)
            DepartureTile(
                data = tileData,
                onTap = {
                    val upcoming = tileData.upcoming
                    updateTripFilter(
                        TripDetailsFilter(
                            tripId = upcoming.trip.id,
                            vehicleId = upcoming.prediction?.vehicleId,
                            stopSequence = upcoming.stopSequence,
                            selectionLock = false
                        )
                    )
                    analytics.tappedDeparture(
                        routeId = patternsByStop.routeIdentifier,
                        stopId = patternsByStop.stop.id,
                        pinned = pinned,
                        alert = alerts.isNotEmpty(),
                        routeType = patternsByStop.representativeRoute.type,
                        noTrips = null
                    )
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                },
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                showRoutePill = patternsByStop.line != null,
                showHeadsign = showTileHeadsigns,
                isSelected = tileData.upcoming.trip.id == tripFilter?.tripId
            )
        }
    }
}
