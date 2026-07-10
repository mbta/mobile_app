package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorLazyColumn
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Composable
fun ColumnScope.StopCardList(
    stopCardData: List<StopCardData>?,
    emptyView: @Composable () -> Unit,
    global: GlobalResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    val contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp)
    val verticalArrangement = Arrangement.spacedBy(14.dp)
    val horizontalAlignment = Alignment.CenterHorizontally
    if (stopCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            ScrollSeparatorLazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                userScrollEnabled = false,
            ) {
                items(5) { LoadingStopCard() }
            }
        }
    } else if (stopCardData.isEmpty()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            emptyView()
        }
    } else {
        ScrollSeparatorLazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            items(stopCardData) { StopCard(it, global, now, isFavorite, onOpenStopDetails) }
        }
    }
}

@Preview
@Composable
private fun StopCardListPreview() {
    val now = EasternTimeInstant.now()
    val objects = TestData.clone("StopCardListPreview")

    val ruggles = objects.getStop("place-rugg")
    val tremontAtMelneaCass = objects.stop {
        id = "1227"
        locationType = LocationType.STOP
        name = "Tremont St @ Melnea Cass Blvd"
        wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
    }
    val boylston = objects.getStop("place-boyls")
    val ol = LineOrRoute.Route(objects.getRoute("Orange"))
    val olSouthboundPattern = objects.getRoutePattern("Orange-3-0")
    val olNorthboundPattern = objects.getRoutePattern("Orange-3-1")
    val gl =
        LineOrRoute.Line(
            objects.getLine("line-Green"),
            setOf(
                objects.getRoute("Green-B"),
                objects.getRoute("Green-C"),
                objects.getRoute("Green-D"),
                objects.getRoute("Green-E"),
            ),
        )
    val bus43 =
        LineOrRoute.Route(
            objects.route {
                id = "43"
                color = "FFC72C"
                directionNames = listOf("Outbound", "Inbound")
                directionDestinations = listOf("Ruggles Station", "Park Street Station")
                shortName = "43"
                textColor = "000000"
                type = RouteType.BUS
            }
        )
    val bus43Inbound =
        objects.routePattern(bus43.route) {
            directionId = 1
            sortOrder = 504301000
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "Park St & Tremont St"
                stopIds = listOf("17869", "1227", "10000")
            }
        }

    val global = GlobalResponse(objects)

    val olSouthboundLeaf =
        RouteCardData.Leaf(
            ol,
            ruggles,
            Direction(0, ol.route),
            listOf(olSouthboundPattern),
            ruggles.childStopIds.toSet(),
            upcomingTrips =
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(olSouthboundPattern)
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(olSouthboundPattern)
                            departureTime = now + 11.minutes
                        }
                    ),
                ),
            alertsHere = emptyList(),
            allDataLoaded = true,
            hasSchedulesToday = true,
            subwayServiceStartTime = null,
            alertsDownstream = listOf(objects.alert { effect = Alert.Effect.Suspension }),
            RouteCardData.Context.Favorites,
        )

    val olNorthboundLeaf =
        RouteCardData.Leaf(
            ol,
            ruggles,
            Direction(1, ol.route),
            listOf(olNorthboundPattern),
            ruggles.childStopIds.toSet(),
            upcomingTrips =
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(olNorthboundPattern)
                            departureTime = now + 6.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(olNorthboundPattern)
                            departureTime = now + 14.minutes
                        }
                    ),
                ),
            alertsHere = emptyList(),
            allDataLoaded = true,
            hasSchedulesToday = true,
            subwayServiceStartTime = null,
            alertsDownstream = emptyList(),
            RouteCardData.Context.Favorites,
        )

    val bus43RugglesLeaf =
        RouteCardData.Leaf(
            bus43,
            ruggles,
            Direction(1, bus43.route),
            listOf(bus43Inbound),
            ruggles.childStopIds.toSet(),
            upcomingTrips =
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 22.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 56.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 12.hours
                        }
                    ),
                ),
            alertsHere = emptyList(),
            allDataLoaded = true,
            hasSchedulesToday = true,
            subwayServiceStartTime = null,
            alertsDownstream = emptyList(),
            RouteCardData.Context.Favorites,
        )

    val bus43TremontAtMelneaCassLeaf =
        RouteCardData.Leaf(
            bus43,
            tremontAtMelneaCass,
            Direction(1, bus43.route),
            listOf(bus43Inbound),
            setOf(tremontAtMelneaCass.id),
            upcomingTrips =
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 25.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 59.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(bus43Inbound)
                            departureTime = now + 12.hours
                        }
                    ),
                ),
            alertsHere = emptyList(),
            allDataLoaded = true,
            hasSchedulesToday = true,
            subwayServiceStartTime = null,
            alertsDownstream = emptyList(),
            RouteCardData.Context.Favorites,
        )

    val glWestboundLeaf =
        RouteCardData.Leaf(
            gl,
            boylston,
            Direction("West", null, 0),
            listOf(
                objects.getRoutePattern("Green-B-812-0"),
                objects.getRoutePattern("Green-C-832-0"),
                objects.getRoutePattern("Green-D-855-0"),
                objects.getRoutePattern("Green-E-886-0"),
            ),
            boylston.childStopIds.toSet(),
            upcomingTrips =
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(objects.getRoutePattern("Green-C-832-0"))
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(objects.getRoutePattern("Green-B-812-0"))
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(objects.getRoutePattern("Green-D-855-0"))
                            departureTime = now + 10.minutes
                        }
                    ),
                ),
            alertsHere = emptyList(),
            allDataLoaded = true,
            hasSchedulesToday = true,
            subwayServiceStartTime = null,
            alertsDownstream = emptyList(),
            RouteCardData.Context.Favorites,
        )

    try {
        stopKoin()
    } catch (_: Exception) {}
    startKoin {
        modules(
            module {
                single<Analytics> { MockAnalytics() }
                single {
                    SettingsCache(
                        MockSettingsRepository(
                            mapOf(
                                Settings.FavoritesByStop to true,
                                Settings.StationAccessibility to true,
                            )
                        )
                    )
                }
            }
        )
    }
    Column {
        StopCardList(
            listOf(
                StopCardData(ruggles, listOf(olSouthboundLeaf, olNorthboundLeaf, bus43RugglesLeaf)),
                StopCardData(tremontAtMelneaCass, listOf(bus43TremontAtMelneaCassLeaf)),
                StopCardData(boylston, listOf(glWestboundLeaf)),
            ),
            emptyView = {},
            global,
            now,
            isFavorite = { true },
            onOpenStopDetails = { _, _ -> },
        )
    }
}
