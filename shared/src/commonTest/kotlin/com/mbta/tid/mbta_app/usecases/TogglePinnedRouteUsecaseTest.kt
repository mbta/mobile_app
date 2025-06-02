package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.MockPinnedRoutesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest

class TogglePinnedRouteUsecaseTest : KoinTest {

    @Test
    fun testTogglePinnedRoute() = runBlocking {
        val repository = MockPinnedRoutesRepository()
        val usecase = TogglePinnedRouteUsecase(repository)
        val routeId = "Red"
        assertTrue { repository.getPinnedRoutes().isEmpty() }
        usecase.execute(routeId)
        assertEquals(repository.getPinnedRoutes(), setOf(routeId))
        usecase.execute(routeId)
        assertTrue { repository.getPinnedRoutes().isEmpty() }
    }
}
