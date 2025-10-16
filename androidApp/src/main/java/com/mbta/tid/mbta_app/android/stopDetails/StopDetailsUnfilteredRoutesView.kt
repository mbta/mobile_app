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
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCard
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.MockErrorBannerViewModel
import kotlin.time.Duration.Companion.minutes
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun StopDetailsUnfilteredRoutesView(
    stop: Stop,
    routeCardData: List<RouteCardData>,
    servedRoutes: List<PillFilter>,
    errorBannerViewModel: IErrorBannerViewModel,
    now: EasternTimeInstant,
    globalData: GlobalResponse?,
    isFavorite: (RouteStopDirection) -> Boolean?,
    onClose: () -> Unit,
    onTapRoutePill: (PillFilter) -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit,
) {
    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)
    val elevatorAlerts =
        routeCardData.flatMap { it.stopData.flatMap { it.elevatorAlerts } }.distinct()
    val hasAccessibilityWarning = elevatorAlerts.isNotEmpty() || !stop.isWheelchairAccessible
    val multiRoute = servedRoutes.size > 1

    Column(
        Modifier.background(colorResource(R.color.fill2)),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Column(Modifier.heightIn(min = 48.dp)) {
            SheetHeader(
                if (!multiRoute) Modifier.padding(bottom = 8.dp) else Modifier,
                title = stop.name,
                onClose = onClose,
            )
            if (multiRoute) {
                Box(Modifier.height(56.dp).fillMaxWidth()) {
                    StopDetailsFilterPills(
                        servedRoutes = servedRoutes,
                        onTapRoutePill = onTapRoutePill,
                        onClearFilter = { updateStopFilter(null) },
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding =
                    PaddingValues(start = 15.dp, top = 17.dp, end = 15.dp, bottom = 16.dp),
            ) {
                if (showStationAccessibility && hasAccessibilityWarning) {
                    item {
                        Column(Modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (elevatorAlerts.isNotEmpty()) {
                                elevatorAlerts.map {
                                    AlertCard(
                                        it,
                                        null,
                                        AlertCardSpec.Elevator,
                                        Color.Unspecified,
                                        MaterialTheme.colorScheme.onPrimary,
                                        {
                                            openModal(
                                                ModalRoutes.AlertDetails(it.id, null, null, stop.id)
                                            )
                                        },
                                    )
                                }
                            } else {
                                NotAccessibleCard()
                            }
                        }
                    }
                }
                items(routeCardData, key = { it.lineOrRoute.id.idText }) { routeCardData ->
                    RouteCard(
                        routeCardData,
                        globalData,
                        now,
                        isFavorite = { rsd -> isFavorite(rsd) },
                        showStopHeader = false,
                        onOpenStopDetails = { _, stopDetailsFilter ->
                            updateStopFilter(stopDetailsFilter)
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StopDetailsRoutesViewPreview() {
    val now = EasternTimeInstant.now()
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
    val trip1 = objects.trip { headsign = "A" }
    val prediction1 =
        objects.prediction {
            trip = trip1
            departureTime = now + 5.minutes
        }
    val trip2 = objects.trip { headsign = "C" }
    val schedule2 =
        objects.schedule {
            trip = trip2
            departureTime = now + 10.minutes
        }
    val trip3 = objects.trip { headsign = "B" }
    val prediction2 =
        objects.prediction {
            trip = trip3
            departureTime = now + 8.minutes
        }
    val trip4 = objects.trip { headsign = "D" }
    val schedule3 =
        objects.schedule {
            trip = trip4
            departureTime = now + 10.minutes
        }
    val prediction3 =
        objects.prediction {
            trip = trip4
            departureTime = null
            arrivalTime = null
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }

    val globalData = GlobalResponse(objects)

    val lineOrRoute1 = LineOrRoute.Route(route1)
    val lineOrRoute2 = LineOrRoute.Route(route2)
    val context = RouteCardData.Context.StopDetailsUnfiltered
    val routeCardData =
        listOf(
            RouteCardData(
                lineOrRoute1,
                listOf(
                    RouteCardData.RouteStopData(
                        route1,
                        stop,
                        listOf(
                            RouteCardData.Leaf(
                                lineOrRoute1,
                                stop,
                                0,
                                routePatterns = emptyList(),
                                stopIds = emptySet(),
                                listOf(UpcomingTrip(trip1, prediction1)),
                                alertsHere = emptyList(),
                                allDataLoaded = true,
                                hasSchedulesToday = true,
                                alertsDownstream = emptyList(),
                                context = context,
                            )
                        ),
                        globalData,
                    )
                ),
                now,
            ),
            RouteCardData(
                lineOrRoute2,
                listOf(
                    RouteCardData.RouteStopData(
                        route2,
                        stop,
                        listOf(
                            RouteCardData.Leaf(
                                lineOrRoute2,
                                stop,
                                0,
                                routePatterns = emptyList(),
                                stopIds = emptySet(),
                                listOf(
                                    UpcomingTrip(trip3, prediction = prediction2),
                                    UpcomingTrip(trip2, schedule2),
                                ),
                                alertsHere = emptyList(),
                                allDataLoaded = true,
                                hasSchedulesToday = true,
                                alertsDownstream = emptyList(),
                                context = context,
                            ),
                            RouteCardData.Leaf(
                                lineOrRoute2,
                                stop,
                                1,
                                routePatterns = emptyList(),
                                stopIds = emptySet(),
                                listOf(UpcomingTrip(trip4, schedule3, prediction3)),
                                alertsHere = emptyList(),
                                allDataLoaded = true,
                                hasSchedulesToday = true,
                                alertsDownstream = emptyList(),
                                context = context,
                            ),
                        ),
                        globalData,
                    )
                ),
                now,
            ),
        )

    val koin = koinApplication {
        modules(
            module {
                single<Analytics> { MockAnalytics() }
                single<SettingsCache> { SettingsCache(MockSettingsRepository()) }
            }
        )
    }
    val errorBannerVM: IErrorBannerViewModel = MockErrorBannerViewModel()

    MyApplicationTheme {
        KoinContext(koin.koin) {
            StopDetailsUnfilteredRoutesView(
                stop,
                routeCardData,
                listOf(PillFilter.ByRoute(route1, null), PillFilter.ByRoute(route2, null)),
                errorBannerVM,
                now = now,
                globalData,
                isFavorite = { false },
                onClose = {},
                onTapRoutePill = {},
                updateStopFilter = {},
                openModal = {},
            )
        }
    }
}
