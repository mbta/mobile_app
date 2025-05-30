package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest

class FavoritesUsecasesTests : KoinTest {

    @Test
    fun testToggleRouteStopDirection() = runBlocking {
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository)
        val routeStopDirection = RouteStopDirection("Red", "place-alfcl", 0)
        println(repository.getFavorites().routeStopDirection)
        assertNull(repository.getFavorites().routeStopDirection)
        usecase.toggleRouteStopDirection(routeStopDirection)
        assertEquals(repository.getFavorites().routeStopDirection, setOf(routeStopDirection))
        usecase.toggleRouteStopDirection(routeStopDirection)
        assertTrue { repository.getFavorites().routeStopDirection?.isEmpty() == true }
    }

    @Test
    fun testGetRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection = RouteStopDirection("Red", "place-alfcl", 0)
        val savedFavorites = Favorites(setOf(routeStopDirection))
        val repository = MockFavoritesRepository(savedFavorites)
        val usecase = FavoritesUsecases(repository)
        assertEquals(usecase.getRouteStopDirectionFavorites(), setOf(routeStopDirection))
    }

    @Test
    fun testGetEmptyRouteStopDirectionFavorites() = runBlocking {
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository)
        assertEquals(usecase.getRouteStopDirectionFavorites(), emptySet())
    }

    @Test
    fun testAddRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection1 = RouteStopDirection("Red", "place-alfcl", 0)
        val routeStopDirection2 = RouteStopDirection("Red", "place-pktrm", 1)
        val routeStopDirection3 = RouteStopDirection("Red", "place-brntn", 0)
        val favorites = setOf(routeStopDirection1, routeStopDirection2, routeStopDirection3)
        val repository = MockFavoritesRepository()
        val usecase = FavoritesUsecases(repository)
        usecase.addRouteStopDirections(favorites)
        assertEquals(usecase.getRouteStopDirectionFavorites(), favorites)
    }

    @Test
    fun testRemoveRouteStopDirectionFavorites() = runBlocking {
        val routeStopDirection1 = RouteStopDirection("Red", "place-alfcl", 0)
        val routeStopDirection2 = RouteStopDirection("Red", "place-pktrm", 1)
        val routeStopDirection3 = RouteStopDirection("Red", "place-brntn", 0)
        val favorites = setOf(routeStopDirection1, routeStopDirection2, routeStopDirection3)
        val repository = MockFavoritesRepository(Favorites(favorites))
        val usecase = FavoritesUsecases(repository)
        usecase.removeRouteStopDirections(favorites)
        assertEquals(usecase.getRouteStopDirectionFavorites(), emptySet())
    }
}
