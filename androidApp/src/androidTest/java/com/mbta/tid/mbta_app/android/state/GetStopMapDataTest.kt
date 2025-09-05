package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IStopRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetStopMapDataTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopMapData() {
        val builder = ObjectCollectionBuilder()
        val stop = builder.stop()
        val stopMapResponse = StopMapResponse(listOf(), mapOf(stop.id to stop))

        val stopRepository =
            object : IStopRepository {
                override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> {
                    return ApiResult.Ok(stopMapResponse)
                }
            }

        var actualStopMapResponse: StopMapResponse? = null
        composeTestRule.setContent {
            actualStopMapResponse = getStopMapData(stop.id, stopRepository)
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { stopMapResponse == actualStopMapResponse }
        assertEquals(stopMapResponse, actualStopMapResponse)
    }
}
