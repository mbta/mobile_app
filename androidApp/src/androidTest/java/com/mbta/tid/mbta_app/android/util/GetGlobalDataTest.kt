package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GetGlobalDataTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testGlobal() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern = objects.routePattern(route)
        val globalData = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))

        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        val globalRepo =
            object : IGlobalRepository {
                override suspend fun getGlobalData(): GlobalResponse {
                    requestSync.receive()
                    return globalData
                }
            }

        var actualData: GlobalResponse? = globalData
        composeTestRule.setContent { actualData = getGlobalData(globalRepo) }

        composeTestRule.awaitIdle()
        assertNull(actualData)

        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(globalData, actualData)
    }
}