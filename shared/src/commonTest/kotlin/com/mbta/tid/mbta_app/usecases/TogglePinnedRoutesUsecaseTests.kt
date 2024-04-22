package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.mocks.MockPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class TogglePinnedRoutesUsecaseTests : KoinTest {

    @Test
    fun testTogglePinnedRoute() = runBlocking {
        startKoin {
            modules(module { single<IPinnedRoutesRepository> { MockPinnedRoutesRepository() } })
        }
        val repository: IPinnedRoutesRepository by inject()
        val usecase = TogglePinnedRouteUsecase()
        val routeId = "Red"
        assertTrue { repository.getPinnedRoutes().isEmpty() }
        usecase.execute(routeId)
        assertEquals(repository.getPinnedRoutes(), setOf(routeId))
        usecase.execute(routeId)
        assertTrue { repository.getPinnedRoutes().isEmpty() }
    }
}
