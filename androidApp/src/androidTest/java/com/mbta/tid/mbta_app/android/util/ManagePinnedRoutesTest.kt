package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ManagePinnedRoutesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPinnedRoutes() = runTest {
        val getSync = Channel<Unit>(Channel.RENDEZVOUS)
        val pinnedRoutesRepo =
            object : IPinnedRoutesRepository {
                var getCalls = 0
                var setCalls = Channel<Set<String>>(Channel.RENDEZVOUS)

                override suspend fun getPinnedRoutes(): Set<String> {
                    getSync.receive()
                    getCalls++
                    return setOf("$getCalls")
                }

                override suspend fun setPinnedRoutes(routes: Set<String>) {
                    setCalls.send(routes)
                }
            }
        val togglePinnedRouteUsecase = TogglePinnedRouteUsecase(pinnedRoutesRepo)

        var mpr: ManagedPinnedRoutes? = null
        composeTestRule.setContent {
            mpr = managePinnedRoutes(pinnedRoutesRepo, togglePinnedRouteUsecase)
        }

        composeTestRule.awaitIdle()
        assertNotNull(mpr)
        assertNull(mpr!!.pinnedRoutes)
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("1"), mpr!!.pinnedRoutes)

        mpr!!.togglePinnedRoute("place-a")
        getSync.send(Unit)
        assertEquals(setOf("2", "place-a"), pinnedRoutesRepo.setCalls.receive())
        assertEquals(setOf("1"), mpr!!.pinnedRoutes)
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("3"), mpr!!.pinnedRoutes)

        mpr!!.togglePinnedRoute("4")
        getSync.send(Unit)
        assertEquals(emptySet<String>(), pinnedRoutesRepo.setCalls.receive())
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("5"), mpr!!.pinnedRoutes)
    }
}
