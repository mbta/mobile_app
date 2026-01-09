package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSignificance
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun StopDetailsFilteredDeparturesView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    leaf: RouteCardData.Leaf,
    selectedDirection: Direction,
    allAlerts: AlertsStreamDataResponse?,
    now: EasternTimeInstant,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    isFavorite: Boolean,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    viewModel: IStopDetailsViewModel = koinInject(),
    analytics: Analytics = koinInject(),
) {
    val global = getGlobalData("StopDetailsFilteredDeparturesView")
    val leafFormat = remember(leaf, now, global) { leaf.format(now, global) }
    val tileData = leafFormat.tileData(selectedDirection.destination)
    val noPredictionsStatus = leafFormat.noPredictionsStatus()
    val isAllServiceDisrupted = leafFormat.isAllServiceDisrupted

    val lineOrRoute = leaf.lineOrRoute
    val stop = leaf.stop

    val state by viewModel.models.collectAsState()
    val alertSummaries = state.alertSummaries

    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)

    val (elevatorAlerts, alertsHere) =
        leaf.alertsHere(tripId = tripFilter?.tripId).partition {
            it.effect == Alert.Effect.ElevatorClosure
        }
    val hasAccessibilityWarning = (elevatorAlerts.isNotEmpty() || !stop.isWheelchairAccessible)

    val downstreamAlerts: List<Alert> = leaf.alertsDownstream(tripId = tripFilter?.tripId)

    val selectedTripIsCancelled =
        if (tripFilter != null)
            leaf.upcomingTrips.any { it.trip.id == tripFilter.tripId && it.isCancelled }
        else false

    val routeAccents = TripRouteAccents(lineOrRoute.sortRoute)

    // keys are trip IDs
    val bringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }

    val patternsHere =
        remember(leaf) { leaf.routePatterns.filter { it.directionId == stopFilter.directionId } }

    fun openAlertDetails(alert: Alert, spec: AlertCardSpec) {
        val lineId: Line.Id?
        val routeIds: List<Route.Id>

        when (lineOrRoute) {
            is LineOrRoute.Line -> {
                lineId = lineOrRoute.id
                routeIds = lineOrRoute.routes.map { it.id }
            }
            is LineOrRoute.Route -> {
                lineId = null
                routeIds = listOf(lineOrRoute.id)
            }
        }

        openModal(
            ModalRoutes.AlertDetails(
                alertId = alert.id,
                lineId = if (spec == AlertCardSpec.Elevator) null else lineId,
                routeIds = if (spec == AlertCardSpec.Elevator) null else routeIds,
                stopId = stop.id,
            )
        )
    }

    suspend fun updateAlertSummaries(clearExisting: Boolean = false) {
        if (global == null) return
        if (clearExisting) viewModel.setAlertSummaries(emptyMap())

        viewModel.setAlertSummaries(
            (alertsHere + downstreamAlerts).associate {
                it.id to it.summary(stopId, stopFilter.directionId, patternsHere, now, global)
            }
        )
    }

    LaunchedEffect(stopId, stopFilter.directionId) { updateAlertSummaries(clearExisting = true) }
    LaunchedEffect(global, alertsHere, downstreamAlerts, patternsHere, now) {
        updateAlertSummaries()
    }

    LaunchedEffect(tripFilter) {
        val selectedTileId = tileData.firstOrNull { it.isSelected(tripFilter) }?.id
        if (selectedTileId != null) {
            bringIntoViewRequesters[selectedTileId]?.bringIntoView()
        }
    }

    DebugView {
        Column(
            Modifier.align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("stop id: $stopId")
            Text("trip id: ${tripFilter?.tripId ?: "null"}")
            Text("vehicle id: ${tripFilter?.vehicleId ?: "null"}")
        }
    }
    Column(Modifier.navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isAllServiceDisrupted && tileData.isNotEmpty()) {
            DepartureTiles(
                tripFilter,
                lineOrRoute,
                stop,
                tileData,
                downstreamAlerts,
                updateTripFilter,
                isFavorite,
                analytics,
                bringIntoViewRequesters,
                tileScrollState,
            )
        }

        @Composable
        fun AlertCard(alert: Alert, summary: AlertSummary?, spec: AlertCardSpec? = null) {
            val significance = alert.significance(now)
            val spec =
                spec
                    ?: if (significance == AlertSignificance.Major && isAllServiceDisrupted) {
                        AlertCardSpec.Major
                    } else if (
                        significance == AlertSignificance.Minor &&
                            alert.effect == Alert.Effect.Delay
                    ) {
                        AlertCardSpec.Delay
                    } else {
                        AlertCardSpec.Secondary
                    }
            AlertCard(
                alert,
                summary,
                spec,
                color = routeAccents.color,
                textColor = routeAccents.textColor,
                onViewDetails = { openAlertDetails(alert, spec) },
            )
        }

        if (
            alertsHere.isNotEmpty() ||
                downstreamAlerts.isNotEmpty() ||
                (showStationAccessibility && hasAccessibilityWarning)
        ) {
            Column(
                Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // for alerts here and downstream, if the alertSummaries are still loading, skip the
                // alert completely, so the header doesn’t flicker before the summary loads
                alertsHere.forEach {
                    AlertCard(
                        it,
                        if (alertSummaries.containsKey(it.id)) alertSummaries[it.id]
                        else return@forEach,
                    )
                }
                downstreamAlerts.forEach {
                    AlertCard(
                        it,
                        if (alertSummaries.containsKey(it.id)) alertSummaries[it.id]
                        else return@forEach,
                        AlertCardSpec.Downstream,
                    )
                }
                if (showStationAccessibility && hasAccessibilityWarning) {
                    if (elevatorAlerts.isNotEmpty()) {
                        elevatorAlerts.forEach { AlertCard(it, null, AlertCardSpec.Elevator) }
                    } else {
                        NotAccessibleCard()
                    }
                }
            }
        }

        if (isAllServiceDisrupted) {
            Box {}
        } else if (noPredictionsStatus != null) {
            Box(modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 12.dp)) {
                val schedulesRepository: ISchedulesRepository = koinInject()
                var nextScheduleResponse: NextScheduleResponse? by remember { mutableStateOf(null) }
                LaunchedEffect(noPredictionsStatus) {
                    nextScheduleResponse =
                        if (
                            noPredictionsStatus == UpcomingFormat.NoTripsFormat.NoSchedulesToday ||
                                noPredictionsStatus ==
                                    UpcomingFormat.NoTripsFormat.ServiceEndedToday
                        ) {
                            val response =
                                schedulesRepository.getNextSchedule(
                                    lineOrRoute,
                                    stopId,
                                    selectedDirection.id,
                                    now,
                                )
                            (response as? ApiResult.Ok)?.data
                        } else null
                }

                StopDetailsNoTripCard(
                    status = noPredictionsStatus,
                    accentColor = routeAccents.color,
                    directionLabel = selectedDirection.destination ?: selectedDirection.name ?: "",
                    routeType = routeAccents.type,
                    now = now,
                    nextScheduleResponse = nextScheduleResponse,
                )
            }
        } else if (selectedTripIsCancelled) {
            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)) {
                StopDetailsIconCard(
                    routeAccents.color,
                    details = { Text(stringResource(R.string.trip_cancelled_details)) },
                    header = { modifier ->
                        Text(stringResource(R.string.trip_cancelled), modifier = modifier)
                    },
                    icon = { modifier ->
                        Icon(
                            painter = routeSlashIcon(routeType = routeAccents.type),
                            contentDescription = null,
                            modifier = modifier.testTag("route_slash_icon"),
                        )
                    },
                )
            }
        } else {
            val tripDetailsPageFilter =
                remember(stopId, stopFilter, tripFilter) {
                    tripFilter?.let { TripDetailsPageFilter(stopId, stopFilter, it) }
                }
            TripDetailsView(
                tripFilter = tripDetailsPageFilter,
                allAlerts = allAlerts,
                alertSummaries = alertSummaries,
                onOpenAlertDetails = { openAlertDetails(it, AlertCardSpec.Downstream) },
                openSheetRoute = openSheetRoute,
                openModal = openModal,
                now = now,
                routeAccents = routeAccents,
                isTripDetailsPage = false,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DepartureTiles(
    tripFilter: TripDetailsFilter?,
    lineOrRoute: LineOrRoute,
    stop: Stop,
    tiles: List<TileData>,
    alerts: List<Alert>,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    isFavorite: Boolean,
    analytics: Analytics,
    bringIntoViewRequesters: MutableMap<String, BringIntoViewRequester>,
    scrollState: ScrollState,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        Modifier.horizontalScroll(scrollState)
            .padding(horizontal = 10.dp)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            selectionLock = false,
                        )
                    )
                    analytics.tappedDeparture(
                        routeId = lineOrRoute.id,
                        stopId = stop.id,
                        pinned = isFavorite,
                        alert = alerts.isNotEmpty(),
                        routeType = lineOrRoute.sortRoute.type,
                        noTrips = null,
                    )
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                },
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                showRoutePill = lineOrRoute is LineOrRoute.Line,
                showHeadsign = true,
                isSelected = tileData.isSelected(tripFilter),
            )
        }
    }
}
