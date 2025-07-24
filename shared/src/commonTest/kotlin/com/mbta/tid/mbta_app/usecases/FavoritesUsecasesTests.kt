package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest

class FavoritesUsecasesTests : KoinTest {

    @Test
    fun testGetRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection = RouteStopDirection("Red", "place-alfcl", 0)
        val savedFavorites = Favorites(setOf(routeStopDirection))
        val repository = MockFavoritesRepository(savedFavorites)
        val usecase = FavoritesUsecases(repository, MockAnalytics())
        assertEquals(usecase.getRouteStopDirectionFavorites(), setOf(routeStopDirection))
    }

    @Test
    fun testGetEmptyRouteStopDirectionFavorites() = runBlocking {
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository, MockAnalytics())
        assertEquals(usecase.getRouteStopDirectionFavorites(), emptySet())
    }

    @Test
    fun testUpdateFavoritesRecordsAnalytics() = runBlocking {
        val repository =
            MockFavoritesRepository(Favorites(setOf(RouteStopDirection("route_1", "stop_1", 0))))

        var eventLogged: String? = null
        val useCase =
            FavoritesUsecases(
                repository,
                MockAnalytics(onLogEvent = { event, attrs -> eventLogged = event }),
            )

        useCase.updateRouteStopDirections(
            mapOf(
                RouteStopDirection("route_1", "stop_1", 0) to false,
                RouteStopDirection("route_1", "stop_1", 1) to true,
            ),
            EditFavoritesContext.Favorites,
            0,
        )

        assertEquals("updated_favorites", eventLogged)
    }
}
