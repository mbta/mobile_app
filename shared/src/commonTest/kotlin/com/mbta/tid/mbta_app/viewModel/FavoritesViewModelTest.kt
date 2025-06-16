package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class FavoritesViewModelTest : KoinTest {
    val objects = ObjectCollectionBuilder()
    val stop1 = objects.stop()
    val stop2 = objects.stop()
    val route1 = objects.route { directionNames = listOf("Outbound", "Inbound") }
    val route2 = objects.route { directionNames = listOf("Outbound", "Inbound") }
    val patterns =
        listOf(Pair(route1, stop1), Pair(route2, stop2)).associate { (route, stop) ->
            route to
                listOf(0, 1).associateWith { directionId ->
                    objects.routePattern(route) {
                        this.directionId = directionId
                        representativeTrip { stopIds = listOf(stop.id) }
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
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(objects)
                        favorites = MockFavoritesRepository(this@FavoritesViewModelTest.favorites)
                        repositoriesBlock()
                    }
                ),
                viewModelModule(),
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `loads empty favorites`() = runTest {
        setUpKoin { favorites = MockFavoritesRepository(Favorites(emptySet())) }
        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(Clock.System.now())
        viewModel.setLocation(stop1.position)

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    favorites = null,
                    isReturningFromBackground = false,
                    routeCardData = null,
                    staticRouteCardData = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    favorites = emptySet(),
                    isReturningFromBackground = false,
                    routeCardData = null,
                    staticRouteCardData = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    favorites = emptySet(),
                    isReturningFromBackground = false,
                    routeCardData = emptyList(),
                    staticRouteCardData = emptyList(),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `loads full favorites with filtered predictions`() = runTest {
        val now = Clock.System.now()
        val objects = objects.clone()
        val predictions =
            listOf(stop1, stop2).associateWith { stop ->
                listOf(route1, route2).associateWith { route ->
                    listOf(0, 1).associateWith { directionId ->
                        val routePattern =
                            objects.routePatterns.values.first {
                                it.routeId == route.id && it.directionId == directionId
                            }
                        objects.prediction {
                            trip = objects.trip(routePattern)
                            stopId = stop.id
                            departureTime = now + 5.minutes
                        }
                    }
                }
            }

        val globalData = GlobalResponse(objects)
        setUpKoin(objects)

        val viewModel: FavoritesViewModel = get()
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setLocation(stop1.position)

        val staticDataTime = Instant.DISTANT_FUTURE
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
                    staticDataTime,
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
                    staticDataTime,
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
        fun FavoritesViewModel.State.tweakStaticDataTime() =
            this.copy(
                staticRouteCardData =
                    this.staticRouteCardData?.map { routeCardData ->
                        routeCardData.copy(at = staticDataTime)
                    }
            )

        testViewModelFlow(viewModel).test {
            assertEquals(
                FavoritesViewModel.State(
                    favorites = null,
                    isReturningFromBackground = false,
                    routeCardData = null,
                    staticRouteCardData = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    favorites = favorites.routeStopDirection,
                    isReturningFromBackground = false,
                    routeCardData = null,
                    staticRouteCardData = null,
                ),
                awaitItem(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    favorites = favorites.routeStopDirection,
                    isReturningFromBackground = false,
                    routeCardData = null,
                    staticRouteCardData = expectedStaticData,
                ),
                awaitItem().tweakStaticDataTime(),
            )
            assertEquals(
                FavoritesViewModel.State(
                    favorites = favorites.routeStopDirection,
                    isReturningFromBackground = false,
                    routeCardData = expectedRealtimeData,
                    staticRouteCardData = expectedStaticData,
                ),
                awaitItem().tweakStaticDataTime(),
            )
        }
    }
}
