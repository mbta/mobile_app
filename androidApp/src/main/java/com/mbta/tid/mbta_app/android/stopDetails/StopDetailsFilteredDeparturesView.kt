package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
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
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingFormat.NoTripsFormat
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

sealed interface FilteredDeparturesData {
    data class PreGroupByDirection(val patternsByStop: PatternsByStop) : FilteredDeparturesData

    data class PostGroupByDirection(
        val routeCardData: RouteCardData,
        val routeStopData: RouteCardData.RouteStopData,
        val leaf: RouteCardData.Leaf
    ) : FilteredDeparturesData
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopDetailsFilteredDeparturesView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    data: FilteredDeparturesData,
    tileData: List<TileData>,
    noPredictionsStatus: NoTripsFormat?,
    allAlerts: AlertsStreamDataResponse?,
    elevatorAlerts: List<Alert>,
    global: GlobalResponse?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    analytics: Analytics = koinInject()
) {
    val groupByDirection = data is FilteredDeparturesData.PostGroupByDirection
    val lineOrRoute =
        when (data) {
            is FilteredDeparturesData.PreGroupByDirection -> {
                val line = data.patternsByStop.line
                if (line != null) {
                    RouteCardData.LineOrRoute.Line(line, data.patternsByStop.routes.toSet())
                } else {
                    RouteCardData.LineOrRoute.Route(data.patternsByStop.representativeRoute)
                }
            }
            is FilteredDeparturesData.PostGroupByDirection -> data.routeCardData.lineOrRoute
        }
    val stop =
        when (data) {
            is FilteredDeparturesData.PreGroupByDirection -> data.patternsByStop.stop
            is FilteredDeparturesData.PostGroupByDirection -> data.routeStopData.stop
        }
    val availableDirections =
        when (data) {
                is FilteredDeparturesData.PreGroupByDirection ->
                    data.patternsByStop.patterns.map { it.directionId() }
                is FilteredDeparturesData.PostGroupByDirection ->
                    data.routeStopData.data.map { it.directionId }
            }
            .distinct()
            .sorted()
    val directions =
        when (data) {
            is FilteredDeparturesData.PreGroupByDirection -> data.patternsByStop.directions
            is FilteredDeparturesData.PostGroupByDirection -> data.routeStopData.directions
        }

    val expectedDirection = stopFilter.directionId

    val alertSummaries by viewModel.alertSummaries.collectAsState()

    val hideMaps by viewModel.hideMaps.collectAsState()
    val showStationAccessibility by viewModel.showStationAccessibility.collectAsState()

    val hasAccessibilityWarning = (elevatorAlerts.isNotEmpty() || !stop.isWheelchairAccessible)

    val alertsHere: List<Alert> =
        when (data) {
            is FilteredDeparturesData.PreGroupByDirection ->
                if (global != null) {
                    data.patternsByStop.alertsHereFor(
                        directionId = expectedDirection,
                        tripId = tripFilter?.tripId,
                        global = global
                    )
                } else {
                    emptyList()
                }
            is FilteredDeparturesData.PostGroupByDirection -> data.leaf.alertsHere
        }
    val pinned = pinnedRoutes.contains(lineOrRoute.id)

    val downstreamAlerts: List<Alert> =
        when (data) {
            is FilteredDeparturesData.PreGroupByDirection ->
                if (global != null) data.patternsByStop.alertsDownstream(expectedDirection)
                else emptyList()
            is FilteredDeparturesData.PostGroupByDirection -> data.leaf.alertsDownstream
        }

    val selectedTripIsCancelled =
        if (tripFilter != null)
            when (data) {
                is FilteredDeparturesData.PreGroupByDirection ->
                    data.patternsByStop.tripIsCancelled(tripFilter.tripId)
                is FilteredDeparturesData.PostGroupByDirection ->
                    data.leaf.upcomingTrips.any {
                        it.trip.id == tripFilter.tripId && it.isCancelled
                    }
            }
        else false

    val hasMajorAlert = alertsHere.any { it.significance == AlertSignificance.Major }

    val routeHex: String = lineOrRoute.backgroundColor
    val routeColor: Color = Color.fromHex(routeHex)
    val routeType: RouteType = lineOrRoute.sortRoute.type
    val textHex: String = lineOrRoute.textColor
    val routeTextColor: Color = Color.fromHex(textHex)

    // keys are trip IDs
    val bringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }

    val patternsHere =
        remember(data) {
            when (data) {
                    is FilteredDeparturesData.PreGroupByDirection ->
                        data.patternsByStop.patterns.flatMap { it.patterns }
                    is FilteredDeparturesData.PostGroupByDirection -> data.leaf.routePatterns
                }
                .filterNotNull()
                .filter { it.directionId == stopFilter.directionId }
        }

    fun openAlertDetails(alert: Alert, spec: AlertCardSpec) {
        val lineId: String?
        val routeIds: List<String>

        when (lineOrRoute) {
            is RouteCardData.LineOrRoute.Line -> {
                lineId = lineOrRoute.id
                routeIds = lineOrRoute.routes.map { it.id }
            }
            is RouteCardData.LineOrRoute.Route -> {
                lineId = null
                routeIds = listOf(lineOrRoute.id)
            }
        }

        openModal(
            ModalRoutes.AlertDetails(
                alertId = alert.id,
                lineId = if (spec == AlertCardSpec.Elevator) null else lineId,
                routeIds = if (spec == AlertCardSpec.Elevator) null else routeIds,
                stopId = stop.id
            )
        )
    }

    LaunchedEffect(
        global,
        alertsHere,
        downstreamAlerts,
        stopId,
        stopFilter.directionId,
        patternsHere,
        now
    ) {
        if (global == null) return@LaunchedEffect
        viewModel.setAlertSummaries(
            (alertsHere + downstreamAlerts).associate {
                it.id to it.summary(stopId, stopFilter.directionId, patternsHere, now, global)
            }
        )
    }

    LaunchedEffect(tripFilter) {
        if (tripFilter != null) {
            bringIntoViewRequesters[tripFilter.tripId]?.bringIntoView()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        StopDetailsFilteredHeader(
            lineOrRoute.sortRoute,
            (lineOrRoute as? RouteCardData.LineOrRoute.Line)?.line,
            stop,
            pinned = pinned,
            onPin = { togglePinnedRoute(lineOrRoute.id) },
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
                    availableDirections = availableDirections,
                    directions = directions,
                    route = lineOrRoute.sortRoute,
                    line = (lineOrRoute as? RouteCardData.LineOrRoute.Line)?.line,
                    stopFilter,
                    updateStopFilter,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                if (!hasMajorAlert && tileData.isNotEmpty()) {
                    DepartureTiles(
                        tripFilter,
                        groupByDirection,
                        lineOrRoute,
                        stop,
                        tileData,
                        downstreamAlerts,
                        updateTripFilter,
                        pinned,
                        analytics,
                        bringIntoViewRequesters,
                        tileScrollState
                    )
                }

                @Composable
                fun AlertCard(alert: Alert, summary: AlertSummary?, spec: AlertCardSpec? = null) {
                    val spec =
                        spec
                            ?: if (alert.significance == AlertSignificance.Major) {
                                AlertCardSpec.Major
                            } else if (
                                alert.significance == AlertSignificance.Minor &&
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
                        color = routeColor,
                        textColor = routeTextColor,
                        onViewDetails = { openAlertDetails(alert, spec) }
                    )
                }

                if (
                    alertsHere.isNotEmpty() ||
                        downstreamAlerts.isNotEmpty() ||
                        (showStationAccessibility && hasAccessibilityWarning)
                ) {
                    Column(
                        Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // for alerts here and downstream, if the alertSummaries are still loading,
                        // skip the alert completely, so the header doesn’t flicker before the
                        // summary loads
                        alertsHere.forEach {
                            AlertCard(
                                it,
                                if (alertSummaries.containsKey(it.id)) alertSummaries[it.id]
                                else return@forEach
                            )
                        }
                        downstreamAlerts.forEach {
                            AlertCard(
                                it,
                                if (alertSummaries.containsKey(it.id)) alertSummaries[it.id]
                                else return@forEach,
                                AlertCardSpec.Downstream
                            )
                        }
                        if (showStationAccessibility && hasAccessibilityWarning) {
                            if (elevatorAlerts.isNotEmpty()) {
                                elevatorAlerts.forEach {
                                    AlertCard(it, null, AlertCardSpec.Elevator)
                                }
                            } else {
                                NotAccessibleCard()
                            }
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
                        allAlerts = allAlerts,
                        alertSummaries = alertSummaries,
                        stopDetailsVM = viewModel,
                        onOpenAlertDetails = { openAlertDetails(it, AlertCardSpec.Downstream) },
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
    groupByDirection: Boolean,
    lineOrRoute: RouteCardData.LineOrRoute,
    stop: Stop,
    tiles: List<TileData>,
    alerts: List<Alert>,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    pinned: Boolean,
    analytics: Analytics,
    bringIntoViewRequesters: MutableMap<String, BringIntoViewRequester>,
    scrollState: ScrollState
) {
    val coroutineScope = rememberCoroutineScope()
    val showTileHeadsigns =
        groupByDirection ||
            (lineOrRoute is RouteCardData.LineOrRoute.Line ||
                !tiles.all { it.headsign == tiles.firstOrNull()?.headsign })
    Row(
        Modifier.horizontalScroll(scrollState).padding(horizontal = 10.dp),
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
                        routeId = lineOrRoute.id,
                        stopId = stop.id,
                        pinned = pinned,
                        alert = alerts.isNotEmpty(),
                        routeType = lineOrRoute.sortRoute.type,
                        noTrips = null
                    )
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                },
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                showRoutePill = lineOrRoute is RouteCardData.LineOrRoute.Line,
                showHeadsign = showTileHeadsigns,
                isSelected = tileData.id == tripFilter?.tripId
            )
        }
    }
}
