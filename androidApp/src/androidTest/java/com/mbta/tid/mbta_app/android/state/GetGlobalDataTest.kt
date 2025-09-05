package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetGlobalDataTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testGlobal() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern = objects.routePattern(route)
        val globalData = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))

        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        val globalRepo =
            object : IGlobalRepository {
                override val state: StateFlow<GlobalResponse?> =
                    MutableStateFlow<GlobalResponse?>(globalData).asStateFlow()

                override suspend fun getGlobalData(): ApiResult<GlobalResponse> {
                    requestSync.receive()
                    return ApiResult.Ok(globalData)
                }
            }

        var actualData: GlobalResponse? = null
        composeTestRule.setContent { actualData = getGlobalData("errorKey", globalRepo) }
        // Data should be set immediately from the repo, even before getGlobalData has completed
        composeTestRule.waitUntilDefaultTimeout { globalData == actualData }
        assertEquals(globalData, actualData)

        requestSync.send(Unit)
        composeTestRule.waitUntilDefaultTimeout { globalData == actualData }
        assertEquals(globalData, actualData)
    }

    @Test
    fun testApiError() {
        val globalRepo = MockGlobalRepository(ApiResult.Error(500, "oops"))

        val errorRepo = MockErrorBannerStateRepository()

        composeTestRule.setContent { getGlobalData("errorKey", globalRepo, errorRepo) }

        composeTestRule.waitUntilDefaultTimeout {
            when (val errorState = errorRepo.state.value) {
                is ErrorBannerState.DataError ->
                    errorState.messages == setOf("errorKey.getGlobalData")
                else -> false
            }
        }
    }
}
