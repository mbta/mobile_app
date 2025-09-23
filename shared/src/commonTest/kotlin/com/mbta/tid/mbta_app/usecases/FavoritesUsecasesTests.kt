package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest

class FavoritesUsecasesTests : KoinTest {

    @Test
    fun testGetRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection = RouteStopDirection("Red", "place-alfcl", 0)
        val savedFavorites = buildFavorites { routeStopDirection(routeStopDirection) }
        val repository = MockFavoritesRepository(savedFavorites)
        val usecase = FavoritesUsecases(repository, MockAnalytics())
        assertEquals(
            usecase.getRouteStopDirectionFavorites(),
            mapOf(
                routeStopDirection to
                    FavoriteSettings(notifications = FavoriteSettings.Notifications.disabled)
            ),
        )
    }

    @Test
    fun testGetEmptyRouteStopDirectionFavorites() = runBlocking {
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository, MockAnalytics())
        assertEquals(usecase.getRouteStopDirectionFavorites(), emptyMap())
    }

    @Test
    fun testUpdateFavoritesRecordsAnalytics() = runBlocking {
        val repository =
            MockFavoritesRepository(buildFavorites { routeStopDirection("route_1", "stop_1", 0) })

        var eventLogged: String? = null
        val useCase =
            FavoritesUsecases(
                repository,
                MockAnalytics(onLogEvent = { event, attrs -> eventLogged = event }),
            )

        useCase.updateRouteStopDirections(
            mapOf(
                RouteStopDirection("route_1", "stop_1", 0) to null,
                RouteStopDirection("route_1", "stop_1", 1) to
                    FavoriteSettings(notifications = FavoriteSettings.Notifications.disabled),
            ),
            EditFavoritesContext.Favorites,
            0,
        )

        assertEquals("updated_favorites", eventLogged)
    }
}
