package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCard
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun StopDetailsUnfilteredRoutesView(
    stop: Stop,
    departures: StopDetailsDepartures,
    servedRoutes: List<PillFilter>,
    errorBannerViewModel: ErrorBannerViewModel,
    showElevatorAccessibility: Boolean,
    now: Instant,
    pinRoute: (String) -> Unit,
    pinnedRoutes: Set<String>,
    onClose: () -> Unit,
    onTapRoutePill: (PillFilter) -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit
) {
    Layout(
        stop,
        departures.elevatorAlerts,
        servedRoutes,
        errorBannerViewModel,
        showElevatorAccessibility,
        onClose,
        onTapRoutePill,
        updateStopFilter,
        openModal
    ) {
        items(departures.routes, key = { it.routeIdentifier }) { patternsByStop ->
            StopDetailsRouteView(
                patternsByStop,
                now,
                pinned = pinnedRoutes.contains(patternsByStop.routeIdentifier),
                onPin = pinRoute,
                updateStopFilter
            )
        }
    }
}

@Composable
fun StopDetailsUnfilteredRoutesView(
    stop: Stop,
    routeCardData: List<RouteCardData>,
    servedRoutes: List<PillFilter>,
    errorBannerViewModel: ErrorBannerViewModel,
    showElevatorAccessibility: Boolean,
    now: Instant,
    onClose: () -> Unit,
    onTapRoutePill: (PillFilter) -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit
) {
    Layout(
        stop,
        routeCardData.flatMap { it.stopData.flatMap { it.elevatorAlerts } }.distinct(),
        servedRoutes,
        errorBannerViewModel,
        showElevatorAccessibility,
        onClose,
        onTapRoutePill,
        updateStopFilter,
        openModal
    ) {
        items(routeCardData, key = { it.lineOrRoute.id }) { routeCardData ->
            RouteCard(
                routeCardData,
                now,
                showElevatorAccessibility,
                onOpenStopDetails = { _, stopDetailsFilter -> updateStopFilter(stopDetailsFilter) }
            )
        }
    }
}

@Composable
private fun Layout(
    stop: Stop,
    elevatorAlerts: List<Alert>,
    servedRoutes: List<PillFilter>,
    errorBannerViewModel: ErrorBannerViewModel,
    showElevatorAccessibility: Boolean,
    onClose: () -> Unit,
    onTapRoutePill: (PillFilter) -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    body: LazyListScope.() -> Unit,
) {
    val hasAccessibilityWarning = elevatorAlerts.isNotEmpty() || !stop.isWheelchairAccessible
    Column(
        Modifier.background(colorResource(R.color.fill2)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(Modifier.heightIn(min = 48.dp)) {
            SheetHeader(onClose = onClose, title = stop.name)
            if (servedRoutes.size > 1) {
                Box(Modifier.height(56.dp).fillMaxWidth()) {
                    StopDetailsFilterPills(
                        servedRoutes = servedRoutes,
                        onTapRoutePill = onTapRoutePill,
                        onClearFilter = { updateStopFilter(null) }
                    )
                }
            }
        }

        ErrorBanner(errorBannerViewModel, Modifier.padding(vertical = 16.dp))

        Box(Modifier.background(MaterialTheme.colorScheme.surface).fillMaxSize()) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = 16.dp)
            ) {
                if (showElevatorAccessibility && hasAccessibilityWarning) {
                    item {
                        Column(
                            Modifier.padding(bottom = 14.dp, start = 14.dp, end = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (elevatorAlerts.isNotEmpty()) {
                                elevatorAlerts.map {
                                    AlertCard(
                                        it,
                                        AlertCardSpec.Elevator,
                                        Color.Unspecified,
                                        MaterialTheme.colorScheme.onPrimary,
                                        {
                                            openModal(
                                                ModalRoutes.AlertDetails(it.id, null, null, stop.id)
                                            )
                                        }
                                    )
                                }
                            } else {
                                NotAccessibleCard()
                            }
                        }
                    }
                }
                body()
            }
        }
    }
}

@Preview
@Composable
private fun StopDetailsRoutesViewPreview() {
    val objects = ObjectCollectionBuilder()

    val route1 =
        objects.route {
            color = "00843D"
            longName = "Green Line B"
            shortName = "B"
            textColor = "FFFFFF"
            type = RouteType.LIGHT_RAIL
        }
    val route2 =
        objects.route {
            color = "FFC72C"
            shortName = "57"
            textColor = "000000"
            type = RouteType.BUS
        }
    val stop = objects.stop { name = "Boylston" }
    val trip1 = objects.trip()
    val prediction1 =
        objects.prediction {
            trip = trip1
            departureTime = Clock.System.now() + 5.minutes
        }
    val trip2 = objects.trip()
    val schedule2 =
        objects.schedule {
            trip = trip2
            departureTime = Clock.System.now() + 10.minutes
        }
    val trip3 = objects.trip()
    val prediction2 =
        objects.prediction {
            trip = trip3
            departureTime = Clock.System.now() + 8.minutes
        }
    val trip4 = objects.trip()
    val schedule3 =
        objects.schedule {
            trip = trip4
            departureTime = Clock.System.now() + 10.minutes
        }
    val prediction3 =
        objects.prediction {
            trip = trip4
            departureTime = null
            arrivalTime = null
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }

    val departures =
        StopDetailsDepartures(
            listOf(
                PatternsByStop(
                    route1,
                    stop,
                    listOf(
                        RealtimePatterns.ByHeadsign(
                            route1,
                            "A",
                            null,
                            emptyList(),
                            listOf(UpcomingTrip(trip1, prediction = prediction1))
                        )
                    )
                ),
                PatternsByStop(
                    route2,
                    stop,
                    listOf(
                        RealtimePatterns.ByHeadsign(
                            route2,
                            "B",
                            null,
                            emptyList(),
                            listOf(UpcomingTrip(trip3, prediction = prediction2))
                        ),
                        RealtimePatterns.ByHeadsign(
                            route2,
                            "C",
                            null,
                            emptyList(),
                            listOf(UpcomingTrip(trip2, schedule2))
                        ),
                        RealtimePatterns.ByHeadsign(
                            route2,
                            "D",
                            null,
                            emptyList(),
                            listOf(UpcomingTrip(trip4, schedule3, prediction3))
                        )
                    )
                )
            )
        )

    val koin = koinApplication { modules(module { single<Analytics> { MockAnalytics() } }) }

    MyApplicationTheme {
        KoinContext(koin.koin) {
            StopDetailsUnfilteredRoutesView(
                stop,
                departures,
                listOf(PillFilter.ByRoute(route1, null), PillFilter.ByRoute(route2, null)),
                ErrorBannerViewModel(
                    false,
                    MockErrorBannerStateRepository(),
                    MockSettingsRepository()
                ),
                showElevatorAccessibility = true,
                now = Clock.System.now(),
                pinRoute = {},
                pinnedRoutes = emptySet(),
                onClose = {},
                onTapRoutePill = {},
                updateStopFilter = {},
                openModal = {}
            )
        }
    }
}
