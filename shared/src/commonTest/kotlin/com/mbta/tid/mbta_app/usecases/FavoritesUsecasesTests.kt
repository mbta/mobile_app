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
}
