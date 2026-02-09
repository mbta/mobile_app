package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SubscriptionRequest
import com.mbta.tid.mbta_app.model.WindowRequest
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.koin.test.KoinTest

class FavoritesUsecasesTests : KoinTest {

    @Test
    fun testGetRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection = RouteStopDirection(Route.Id("Red"), "place-alfcl", 0)
        val savedFavorites = buildFavorites { routeStopDirection(routeStopDirection) }
        val repository = MockFavoritesRepository(savedFavorites)
        val usecase = FavoritesUsecases(repository, MockSubscriptionsRepository(), MockAnalytics())
        assertEquals(
            usecase.getRouteStopDirectionFavorites(),
            mapOf(routeStopDirection to FavoriteSettings()),
        )
    }

    @Test
    fun testGetEmptyRouteStopDirectionFavorites() = runBlocking {
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository, MockSubscriptionsRepository(), MockAnalytics())
        assertEquals(usecase.getRouteStopDirectionFavorites(), emptyMap())
    }

    @Test
    fun testUpdateFavoritesRecordsAnalytics() = runBlocking {
        val repository =
            MockFavoritesRepository(
                buildFavorites { routeStopDirection(Route.Id("route_1"), "stop_1", 0) }
            )

        var eventLogged: String? = null
        val useCase =
            FavoritesUsecases(
                repository,
                MockSubscriptionsRepository(),
                MockAnalytics(onLogEvent = { event, attrs -> eventLogged = event }),
            )

        useCase.updateRouteStopDirections(
            mapOf(
                RouteStopDirection(Route.Id("route_1"), "stop_1", 0) to null,
                RouteStopDirection(Route.Id("route_1"), "stop_1", 1) to FavoriteSettings(),
            ),
            EditFavoritesContext.Favorites,
            0,
            null,
            false,
        )

        assertEquals("updated_favorites", eventLogged)
    }

    @Test
    fun testUpdateFavoritesUpdatesSubscriptions() = runBlocking {
        val repository =
            MockFavoritesRepository(
                buildFavorites { routeStopDirection(Route.Id("route_1"), "stop_1", 0) }
            )
        var token: String? = null
        var subs: List<SubscriptionRequest>? = null
        val subscriptionsRepository =
            MockSubscriptionsRepository(
                onUpdateSubscriptions = { fcmToken, subscriptions ->
                    token = fcmToken
                    subs = subscriptions
                }
            )

        val useCase = FavoritesUsecases(repository, subscriptionsRepository, MockAnalytics())

        val expectedSubs =
            listOf(
                SubscriptionRequest(
                    "route_1",
                    "stop_1",
                    0,
                    false,
                    listOf(
                        WindowRequest(
                            startTime = LocalTime(9, 0),
                            endTime = LocalTime(17, 0),
                            listOf(2, 3),
                        )
                    ),
                ),
                SubscriptionRequest(
                    "route_1",
                    "stop_1",
                    1,
                    false,
                    listOf(
                        WindowRequest(
                            startTime = LocalTime(8, 0),
                            endTime = LocalTime(9, 0),
                            listOf(1),
                        )
                    ),
                ),
            )

        val settings1 =
            FavoriteSettings.Notifications(
                enabled = true,
                windows =
                    listOf(
                        FavoriteSettings.Notifications.Window(
                            startTime = LocalTime(9, 0),
                            endTime = LocalTime(17, 0),
                            setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                        )
                    ),
            )

        val settings2 =
            FavoriteSettings.Notifications(
                enabled = true,
                windows =
                    listOf(
                        FavoriteSettings.Notifications.Window(
                            startTime = LocalTime(8, 0),
                            endTime = LocalTime(9, 0),
                            setOf(DayOfWeek.MONDAY),
                        )
                    ),
            )

        useCase.updateRouteStopDirections(
            mapOf(
                RouteStopDirection(Route.Id("route_1"), "stop_1", 0) to FavoriteSettings(settings1),
                RouteStopDirection(Route.Id("route_1"), "stop_1", 1) to FavoriteSettings(settings2),
                RouteStopDirection(Route.Id("route_2"), "stop_2", 0) to
                    FavoriteSettings(FavoriteSettings.Notifications.disabled),
            ),
            EditFavoritesContext.Favorites,
            0,
            "fake_token",
            false,
        )

        delay(250.milliseconds)

        assertEquals("fake_token", token)
        assertEquals(expectedSubs, subs)
    }
}
