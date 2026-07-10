package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalCoroutinesApi::class)
internal class NearbyViewModelTests : KoinTest {
    val objects = ObjectCollectionBuilder()
    val stop1 = objects.stop {
        id = "stop1"
        latitude = 0.0
        longitude = 0.0
    }
    val stop2 = objects.stop {
        id = "stop2"
        latitude = 1.0
        longitude = 1.0
    }
    val stop3 = objects.stop {
        id = "stop3"
        latitude = -0.5
        longitude = -0.5
    }
    val route1 = objects.route {
        id = "route1"
        directionNames = listOf("Outbound", "Inbound")
    }
    val route2 = objects.route {
        id = "route2"
        directionNames = listOf("Outbound", "Inbound")
    }
    val patterns =
        listOf(Pair(route1, listOf(stop1)), Pair(route2, listOf(stop2, stop3))).associate {
            (route, stops) ->
            route to
                listOf(0, 1).associateWith { directionId ->
                    objects.routePattern(route) {
                        this.directionId = directionId
                        representativeTrip { stopIds = stops.map { it.id } }
                    }
                }
        }

    private fun setUpKoin(
        objects: ObjectCollectionBuilder = this.objects,
        coroutineDispatcher: CoroutineDispatcher,
        analytics: Analytics = MockAnalytics(),
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(objects)
                        repositoriesBlock()
                    }
                ),
                viewModelModule(),
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                        coroutineDispatcher
                    }
                    single<Analytics> { analytics }
                    single<Clock> { Clock.System }
                },
            )
        }
    }

    private fun predictionsEverywhere(objects: ObjectCollectionBuilder, now: EasternTimeInstant) =
        listOf(stop1, stop2).associateWith { stop ->
            listOf(route1, route2).associateWith { route ->
                listOf(0, 1).associateWith { directionId ->
                    val routePattern = patterns.getValue(route).getValue(directionId)
                    objects.prediction {
                        trip = objects.trip(routePattern)
                        stopId = stop.id
                        departureTime = now + 5.minutes
                    }
                }
            }
        }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `loads empty nearby data`() = runTest {
        try {
            val dispatcher = StandardTestDispatcher(testScheduler)
            setUpKoin(objects, dispatcher) {
                favorites = MockFavoritesRepository(Favorites(emptyMap()))
            }
        } catch (e: Exception) {}
        val viewModel: NearbyViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())
        viewModel.setLocation(stop1.position)
        viewModel.setActive(true, false)

        testViewModelFlow(viewModel).test {
            assertEquals(
                NearbyViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    routeCardData = null,
                    loadedLocation = null,
                    loadedStopIds = null,
                ),
                awaitItem(),
            )
            assertEquals(
                NearbyViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    routeCardData = null,
                    loadedLocation = stop1.position,
                    loadedStopIds = listOf(stop1.id, stop2.id),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `loads route card data`() = runTest {
        val now = EasternTimeInstant.now()
        val objects = objects.clone()
        val predictions = predictionsEverywhere(objects, now)

        val globalData = GlobalResponse(objects)
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: NearbyViewModel = get()
        viewModel.setActive(active = true, wasSentToBackground = false)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        val expectedRealtimeData =
            listOf(
                RouteCardData(
                    LineOrRoute.Route(route1),
                    listOf(
                        RouteCardData.RouteStopData(
                            route1,
                            stop1,
                            listOf(
                                RouteCardData.Leaf(
                                    LineOrRoute.Route(route1),
                                    stop1,
                                    Direction(0, route1),
                                    listOf(patterns.getValue(route1).getValue(0)),
                                    setOf(stop1.id),
                                    upcomingTrips =
                                        listOf(
                                            objects.upcomingTrip(
                                                predictions
                                                    .getValue(stop1)
                                                    .getValue(route1)
                                                    .getValue(0)
                                            )
                                        ),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.NearbyTransit,
                                ),
                                RouteCardData.Leaf(
                                    LineOrRoute.Route(route1),
                                    stop1,
                                    Direction(1, route1),
                                    listOf(patterns.getValue(route1).getValue(1)),
                                    setOf(stop1.id),
                                    upcomingTrips =
                                        listOf(
                                            objects.upcomingTrip(
                                                predictions
                                                    .getValue(stop1)
                                                    .getValue(route1)
                                                    .getValue(1)
                                            )
                                        ),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.NearbyTransit,
                                ),
                            ),
                        )
                    ),
                    now,
                ),
                RouteCardData(
                    LineOrRoute.Route(route2),
                    listOf(
                        RouteCardData.RouteStopData(
                            route2,
                            stop2,
                            listOf(
                                RouteCardData.Leaf(
                                    LineOrRoute.Route(route2),
                                    stop2,
                                    Direction(0, route2),
                                    listOf(patterns.getValue(route2).getValue(0)),
                                    setOf(stop2.id),
                                    upcomingTrips =
                                        listOf(
                                            objects.upcomingTrip(
                                                predictions
                                                    .getValue(stop2)
                                                    .getValue(route2)
                                                    .getValue(0)
                                            )
                                        ),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.NearbyTransit,
                                ),
                                RouteCardData.Leaf(
                                    LineOrRoute.Route(route2),
                                    stop2,
                                    Direction(1, route2),
                                    listOf(patterns.getValue(route2).getValue(1)),
                                    setOf(stop2.id),
                                    upcomingTrips =
                                        listOf(
                                            objects.upcomingTrip(
                                                predictions
                                                    .getValue(stop2)
                                                    .getValue(route2)
                                                    .getValue(1)
                                            )
                                        ),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.NearbyTransit,
                                ),
                            ),
                        )
                    ),
                    now,
                ),
            )

        testViewModelFlow(viewModel).test {
            assertEquals(
                NearbyViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    routeCardData = null,
                    loadedLocation = null,
                    loadedStopIds = null,
                ),
                awaitItem(),
            )
            assertEquals(
                NearbyViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    routeCardData = null,
                    loadedLocation = stop1.position,
                    loadedStopIds = listOf(stop1.id, stop2.id),
                ),
                awaitItem(),
            )
            awaitItemSatisfying {
                it ==
                    NearbyViewModel.State(
                        false,
                        expectedRealtimeData,
                        stop1.position,
                        listOf(stop1.id, stop2.id),
                    )
            }
        }
    }

    @Test
    fun `disconnects when inactive and awaits predictions in background`() = runTest {
        var predictionsConnected = false

        val dispatcher = StandardTestDispatcher(testScheduler)

        setUpKoin(objects, dispatcher) {
            predictions =
                MockPredictionsRepository(
                    onConnectV2 = { predictionsConnected = true },
                    onDisconnect = { predictionsConnected = false },
                    connectV2Response = PredictionsByStopJoinResponse.empty,
                )
        }

        val viewModel: NearbyViewModel = get()
        viewModel.setActive(true, false)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setLocation(Position(0.0, 0.0))

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying {
                it.routeCardData != null && !it.awaitingPredictionsAfterBackground
            }
            advanceUntilIdle()
            assertTrue(predictionsConnected)
            viewModel.setActive(false, wasSentToBackground = true)
            awaitItemSatisfying {
                it.routeCardData != null && it.awaitingPredictionsAfterBackground
            }
            advanceUntilIdle()
            assertFalse(predictionsConnected)
            viewModel.setActive(true)
            awaitItemSatisfying {
                it.routeCardData != null && !it.awaitingPredictionsAfterBackground
            }
            advanceUntilIdle()
            assertTrue(predictionsConnected)
        }
    }

    @Test
    fun `updates alerts`() = runTest {
        val now = EasternTimeInstant.now()
        val objects = objects.clone()
        predictionsEverywhere(objects, now)

        val alert = objects.alert {
            effect = Alert.Effect.Suspension
            activePeriod(now - 10.minutes, now + 10.minutes)
            informedEntity(stop = stop1.id)
            informedEntity(stop = stop2.id)
        }

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: NearbyViewModel = get()
        viewModel.setActive(true, false)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            assertEquals(
                emptyList(),
                awaitItemSatisfying { it.routeCardData != null }
                    .routeCardData!!
                    .flatMap { it.stopData }
                    .flatMap { it.data }
                    .flatMap { it.alertsHere(tripId = null) },
            )
            viewModel.setAlerts(AlertsStreamDataResponse(objects))
            awaitItemSatisfying { state ->
                listOf(alert, alert, alert, alert) ==
                    state.routeCardData!!
                        .flatMap { it.stopData }
                        .flatMap { it.data }
                        .flatMap { it.alertsHere(tripId = null) }
            }
        }
    }

    @Test
    fun `updates location`() = runTest {
        val now = EasternTimeInstant.now()
        val objects = objects.clone()
        predictionsEverywhere(objects, now)

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: NearbyViewModel = get()
        viewModel.setActive(true, false)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            assertEquals(
                listOf(stop1, stop2),
                awaitItemSatisfying { it.routeCardData != null }
                    .routeCardData!!
                    .flatMap { it.stopData }
                    .map { it.stop },
            )
            viewModel.setLocation(stop2.position)
            advanceUntilIdle()
            awaitItemSatisfying {
                listOf(stop2, stop1) == it.routeCardData!!.flatMap { it.stopData }.map { it.stop }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updates now`() = runTest {
        val now = EasternTimeInstant.now()
        val later = now + 2.minutes
        val objects = objects.clone()
        predictionsEverywhere(objects, now)

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: NearbyViewModel = get()
        viewModel.setActive(true, false)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            assertEquals(
                listOf(now),
                awaitItemSatisfying { it.routeCardData != null }
                    .routeCardData!!
                    .map { it.at }
                    .distinct(),
            )
            viewModel.setNow(later)
            awaitItemSatisfying {
                it.routeCardData?.map { card -> card.at }?.distinct() == listOf(later)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
