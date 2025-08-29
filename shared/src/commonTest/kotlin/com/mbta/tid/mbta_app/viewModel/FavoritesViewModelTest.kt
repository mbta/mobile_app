package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.repositories.DefaultTab
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import dev.mokkery.MockMode
import dev.mokkery.answering.repeat
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentially
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.dellisd.spatialk.geojson.Position
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

@OptIn(ExperimentalCoroutinesApi::class)
internal class FavoritesViewModelTest : KoinTest {
    val objects = ObjectCollectionBuilder()
    val stop1 =
        objects.stop {
            latitude = 0.0
            longitude = 0.0
        }
    val stop2 =
        objects.stop {
            id = "stop1"
            latitude = 1.0
            longitude = 1.0
        }
    val stop3 =
        objects.stop {
            id = "stop2"
            latitude = -0.5
            longitude = -0.5
        }
    val route1 =
        objects.route {
            id = "route1"
            directionNames = listOf("Outbound", "Inbound")
        }
    val route2 =
        objects.route {
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

    val favorites =
        Favorites(
            setOf(
                RouteStopDirection(route1.id, stop1.id, 0),
                RouteStopDirection(route2.id, stop2.id, 1),
            )
        )

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
                        favorites = MockFavoritesRepository(this@FavoritesViewModelTest.favorites)
                        pinnedRoutes = MockPinnedRoutesRepository()
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
    fun `loads empty favorites`() = runTest {
        try {
            val dispatcher = StandardTestDispatcher(testScheduler)
            setUpKoin(objects, dispatcher) {
                favorites = MockFavoritesRepository(Favorites(emptySet()))
            }
        } catch (e: Exception) {
            TODO("Not yet implemented")
        }
        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = null,
                    routeCardData = null,
                    staticRouteCardData = null,
                    loadedLocation = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = emptySet(),
                    routeCardData = null,
                    staticRouteCardData = null,
                    loadedLocation = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = emptySet(),
                    routeCardData = emptyList(),
                    staticRouteCardData = emptyList(),
                    loadedLocation = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `sets default tab to nearby`() = runTest {
        val mockTabPreferencesRepo = mock<ITabPreferencesRepository>(MockMode.autofill)

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            favorites = MockFavoritesRepository(Favorites(emptySet()))
            tabPreferences = mockTabPreferencesRepo
        }
        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying {
                it ==
                    FavoritesViewModel.State(
                        awaitingPredictionsAfterBackground = false,
                        favorites = emptySet(),
                        routeCardData = emptyList(),
                        staticRouteCardData = emptyList(),
                        loadedLocation = null,
                    )
            }
        }

        verifySuspend { mockTabPreferencesRepo.setDefaultTab(DefaultTab.Nearby) }
    }

    @Test
    fun `loads full favorites with filtered predictions`() = runTest {
        val now = EasternTimeInstant.now()
        val objects = objects.clone()
        val predictions = predictionsEverywhere(objects, now)

        val globalData = GlobalResponse(objects)
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        val expectedStaticData =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route1),
                    listOf(
                        RouteCardData.RouteStopData(
                            route1,
                            stop1,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route1),
                                    stop1,
                                    directionId = 0,
                                    listOf(patterns.getValue(route1).getValue(0)),
                                    setOf(stop1.id),
                                    upcomingTrips = emptyList(),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                ),
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route2),
                    listOf(
                        RouteCardData.RouteStopData(
                            route2,
                            stop2,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route2),
                                    stop2,
                                    directionId = 1,
                                    listOf(patterns.getValue(route2).getValue(1)),
                                    setOf(stop2.id),
                                    upcomingTrips = emptyList(),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                ),
            )
        val expectedRealtimeData =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route1),
                    listOf(
                        RouteCardData.RouteStopData(
                            route1,
                            stop1,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route1),
                                    stop1,
                                    directionId = 0,
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
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                ),
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route2),
                    listOf(
                        RouteCardData.RouteStopData(
                            route2,
                            stop2,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route2),
                                    stop2,
                                    directionId = 1,
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
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                ),
            )

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = null,
                    routeCardData = null,
                    staticRouteCardData = null,
                    loadedLocation = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favorites.routeStopDirection,
                    routeCardData = null,
                    staticRouteCardData = null,
                    loadedLocation = null,
                ),
                awaitItem(),
            )
            // static data usually loads before realtime, but not always
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favorites.routeStopDirection,
                    routeCardData = expectedRealtimeData,
                    staticRouteCardData = expectedStaticData,
                    loadedLocation = stop1.position,
                ),
                awaitItemSatisfying { it.routeCardData != null && it.staticRouteCardData != null },
            )
        }
    }

    @Test
    fun `reloads favorites`() = runTest {
        val now = EasternTimeInstant.now()

        val favoritesBefore = Favorites(setOf(RouteStopDirection(route1.id, stop1.id, 0)))
        val favoritesAfter = Favorites(setOf(RouteStopDirection(route2.id, stop2.id, 1)))

        val favoritesRepo = MockFavoritesRepository(favoritesBefore)

        val globalData = GlobalResponse(objects)
        val dispatcher = StandardTestDispatcher(testScheduler)

        setUpKoin(objects, dispatcher) { favorites = favoritesRepo }

        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        val expectedStaticDataBefore =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route1),
                    listOf(
                        RouteCardData.RouteStopData(
                            route1,
                            stop1,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route1),
                                    stop1,
                                    0,
                                    listOf(patterns.getValue(route1).getValue(0)),
                                    setOf(stop1.id),
                                    upcomingTrips = emptyList(),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    alertsDownstream = emptyList(),
                                    RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                )
            )
        val expectedStaticDataAfter =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route2),
                    listOf(
                        RouteCardData.RouteStopData(
                            route2,
                            stop2,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route2),
                                    stop2,
                                    1,
                                    listOf(patterns.getValue(route2).getValue(1)),
                                    setOf(stop2.id),
                                    upcomingTrips = emptyList(),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    alertsDownstream = emptyList(),
                                    RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                )
            )

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favoritesBefore.routeStopDirection,
                    routeCardData = emptyList(),
                    staticRouteCardData = expectedStaticDataBefore,
                    loadedLocation = stop1.position,
                ),
                awaitItemSatisfying {
                    it.routeCardData != null && it.staticRouteCardData == expectedStaticDataBefore
                },
            )
            favoritesRepo.setFavorites(favoritesAfter)
            viewModel.reloadFavorites()
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favoritesAfter.routeStopDirection,
                    routeCardData = emptyList(),
                    staticRouteCardData = expectedStaticDataBefore,
                    loadedLocation = stop1.position,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favoritesAfter.routeStopDirection,
                    routeCardData = emptyList(),
                    staticRouteCardData = expectedStaticDataAfter,
                    loadedLocation = stop1.position,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `updates favorites`() = runTest {
        val now = EasternTimeInstant.now()

        val favoritesBefore = Favorites(setOf(RouteStopDirection(route1.id, stop1.id, 0)))
        val favoritesAfter = Favorites(setOf())

        val favoritesRepo = mock<IFavoritesRepository>(MockMode.autofill)

        everySuspend { favoritesRepo.getFavorites() } sequentially
            {
                returns(favoritesBefore)
                repeat { returns(favoritesAfter) }
            }

        val globalData = GlobalResponse(objects)
        val dispatcher = StandardTestDispatcher(testScheduler)

        setUpKoin(objects, dispatcher) { favorites = favoritesRepo }

        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        val expectedStaticDataBefore =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route1),
                    listOf(
                        RouteCardData.RouteStopData(
                            route1,
                            stop1,
                            listOf(
                                RouteCardData.Leaf(
                                    RouteCardData.LineOrRoute.Route(route1),
                                    stop1,
                                    0,
                                    listOf(patterns.getValue(route1).getValue(0)),
                                    setOf(stop1.id),
                                    upcomingTrips = emptyList(),
                                    alertsHere = emptyList(),
                                    allDataLoaded = true,
                                    hasSchedulesToday = false,
                                    alertsDownstream = emptyList(),
                                    RouteCardData.Context.Favorites,
                                )
                            ),
                            globalData,
                        )
                    ),
                    now,
                )
            )
        val expectedStaticDataAfter: List<RouteCardData> = listOf()

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favoritesBefore.routeStopDirection,
                    routeCardData = emptyList(),
                    staticRouteCardData = expectedStaticDataBefore,
                    loadedLocation = stop1.position,
                ),
                awaitItemSatisfying {
                    it.routeCardData != null && it.staticRouteCardData == expectedStaticDataBefore
                },
            )
            viewModel.updateFavorites(
                mapOf(RouteStopDirection(route1.id, stop1.id, 0) to false),
                EditFavoritesContext.Favorites,
                0,
            )
            awaitItemSatisfying {
                it.routeCardData != null &&
                    it.staticRouteCardData == expectedStaticDataAfter &&
                    (it.favorites == favoritesAfter.routeStopDirection)
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

        val viewModel: FavoritesViewModel = get()
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

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(now - 10.minutes, now + 10.minutes)
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    stop = stop1.id,
                )
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    stop = stop2.id,
                )
            }

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: FavoritesViewModel = get()
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
            assertEquals(
                listOf(alert, alert),
                awaitItem()
                    .routeCardData!!
                    .flatMap { it.stopData }
                    .flatMap { it.data }
                    .flatMap { it.alertsHere(tripId = null) },
            )
        }
    }

    @Test
    fun `updates location`() = runTest {
        val now = EasternTimeInstant.now()
        val objects = objects.clone()
        predictionsEverywhere(objects, now)

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val viewModel: FavoritesViewModel = get()
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

        val viewModel: FavoritesViewModel = get()

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

    @Test
    fun `analytics event when favorites first loaded`() = runTest {
        val now = EasternTimeInstant.now()
        val later = now + 2.minutes
        val objects = objects.clone()
        predictionsEverywhere(objects, now)

        var analyticsLogged: Pair<String, String>? = null
        val mockAnalytics =
            MockAnalytics(
                onSetUserProperty = { event, count -> analyticsLogged = Pair(event, count) }
            )

        val initialFavorites = Favorites(setOf(RouteStopDirection(route1.id, stop1.id, 0)))
        val favoritesRepo = mock<IFavoritesRepository>(MockMode.autofill)
        everySuspend { favoritesRepo.getFavorites() } returns (initialFavorites)

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher, mockAnalytics) { favorites = favoritesRepo }

        val viewModel: FavoritesViewModel = get()

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.favorites == initialFavorites.routeStopDirection }
            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()
        assertEquals(analyticsLogged, Pair("favorites_count", "1"))
    }

    @Test
    fun `does not load new route card data when editing`() = runTest {
        val now = EasternTimeInstant.now()

        val favoritesBefore =
            Favorites(
                setOf(
                    RouteStopDirection(route1.id, stop1.id, 0),
                    RouteStopDirection(route2.id, stop2.id, 1),
                    RouteStopDirection(route2.id, stop3.id, 1),
                )
            )
        val favoritesAfter =
            Favorites(
                setOf(
                    RouteStopDirection(route1.id, stop1.id, 0),
                    RouteStopDirection(route2.id, stop2.id, 1),
                )
            )

        val favoritesRepo = MockFavoritesRepository(favoritesBefore)

        val globalData = GlobalResponse(objects)
        val dispatcher = StandardTestDispatcher(testScheduler)

        setUpKoin(objects, dispatcher) { favorites = favoritesRepo }

        val viewModel: FavoritesViewModel = get()
        viewModel.setContext(FavoritesViewModel.Context.Favorites)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop3.position)

        val stop1Data =
            RouteCardData.RouteStopData(
                route1,
                stop1,
                listOf(
                    RouteCardData.Leaf(
                        RouteCardData.LineOrRoute.Route(route1),
                        stop1,
                        0,
                        listOf(patterns.getValue(route1).getValue(0)),
                        setOf(stop1.id),
                        upcomingTrips = emptyList(),
                        alertsHere = emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = false,
                        alertsDownstream = emptyList(),
                        RouteCardData.Context.Favorites,
                    )
                ),
                globalData,
            )

        val stop2Data =
            RouteCardData.RouteStopData(
                route2,
                stop2,
                listOf(
                    RouteCardData.Leaf(
                        RouteCardData.LineOrRoute.Route(route2),
                        stop2,
                        1,
                        listOf(patterns.getValue(route2).getValue(1)),
                        setOf(stop2.id),
                        upcomingTrips = emptyList(),
                        alertsHere = emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = false,
                        alertsDownstream = emptyList(),
                        RouteCardData.Context.Favorites,
                    )
                ),
                globalData,
            )

        val stop3Data =
            RouteCardData.RouteStopData(
                route2,
                stop3,
                listOf(
                    RouteCardData.Leaf(
                        RouteCardData.LineOrRoute.Route(route2),
                        stop3,
                        1,
                        listOf(patterns.getValue(route2).getValue(1)),
                        setOf(stop3.id),
                        upcomingTrips = emptyList(),
                        alertsHere = emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = false,
                        alertsDownstream = emptyList(),
                        RouteCardData.Context.Favorites,
                    )
                ),
                globalData,
            )
        val routeCard1Data =
            RouteCardData(RouteCardData.LineOrRoute.Route(route1), listOf(stop1Data), now)

        val routeCard2Data =
            RouteCardData(
                RouteCardData.LineOrRoute.Route(route2),
                listOf(stop3Data, stop2Data),
                now,
            )

        val expectedStaticDataBefore = listOf(routeCard2Data, routeCard1Data)

        val expectedStaticDataAfter =
            listOf(routeCard1Data, routeCard2Data.copy(stopData = listOf(stop2Data)))

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    awaitingPredictionsAfterBackground = false,
                    favorites = favoritesBefore.routeStopDirection,
                    routeCardData = emptyList(),
                    staticRouteCardData = expectedStaticDataBefore,
                    loadedLocation = stop3.position,
                ),
                awaitItemSatisfying {
                    it.routeCardData != null && it.staticRouteCardData == expectedStaticDataBefore
                },
            )
            viewModel.setContext(FavoritesViewModel.Context.Edit)
            favoritesRepo.setFavorites(favoritesAfter)
            viewModel.reloadFavorites()
            awaitItemSatisfying {
                it.staticRouteCardData == expectedStaticDataAfter &&
                    it.favorites == favoritesAfter.routeStopDirection
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shouldShowFirstTimeToast true when had pinned routes and is first exposure`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            pinnedRoutes = MockPinnedRoutesRepository(initialPinnedRoutes = setOf("Red"))
        }

        val viewModel: FavoritesViewModel = get()
        viewModel.setIsFirstExposureToNewFavorites(true)

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.shouldShowFirstTimeToast }
            viewModel.setIsFirstExposureToNewFavorites(false)
            awaitItemSatisfying { !it.shouldShowFirstTimeToast }
        }
    }
}
