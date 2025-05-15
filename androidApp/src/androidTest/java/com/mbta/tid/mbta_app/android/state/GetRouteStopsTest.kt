package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.RouteStopsResponse
import com.mbta.tid.mbta_app.repositories.IRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockRouteStopsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GetRouteStopsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRouteStops() {
        fun buildSomeRouteStops(): RouteStopsResponse {
            val objects = ObjectCollectionBuilder()
            return RouteStopsResponse(listOf(objects.stop().id, objects.stop().id))
        }
        val expectedRouteStops1 = buildSomeRouteStops()
        val expectedRouteStops2 = buildSomeRouteStops()

        val routeStopsRepo =
            object : IRouteStopsRepository {
                override suspend fun getRouteStops(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<RouteStopsResponse> {
                    return if (directionId == 0) ApiResult.Ok(expectedRouteStops1)
                    else ApiResult.Ok(expectedRouteStops2)
                }
            }

        var directionId by mutableIntStateOf(0)
        var actualRouteStops: RouteStopsResponse? = expectedRouteStops1
        composeTestRule.setContent {
            actualRouteStops = getRouteStops("", directionId, "errorKey", routeStopsRepo)
        }

        composeTestRule.waitUntil { actualRouteStops != null }
        composeTestRule.waitForIdle()
        assertEquals(expectedRouteStops1, actualRouteStops)

        directionId = 1
        composeTestRule.waitUntil { actualRouteStops != null }
        composeTestRule.waitForIdle()
        assertEquals(expectedRouteStops2, actualRouteStops)
    }

    @Test
    fun testNullWhileLoading(): Unit = runBlocking {
        val sync = Channel<Unit>(capacity = Channel.RENDEZVOUS)
        val routeStopsRepo =
            object : IRouteStopsRepository {
                override suspend fun getRouteStops(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<RouteStopsResponse> {
                    sync.receive()
                    return ApiResult.Ok(RouteStopsResponse(emptyList()))
                }
            }
        var actualRouteStops: RouteStopsResponse? = null

        var directionId by mutableIntStateOf(0)
        composeTestRule.setContent {
            actualRouteStops = getRouteStops("", directionId, "errorKey", routeStopsRepo)
        }

        sync.send(Unit)
        composeTestRule.waitUntil { actualRouteStops != null }
        assertNotNull(actualRouteStops)

        directionId = 1
        composeTestRule.waitForIdle()
        assertNull(actualRouteStops)

        sync.send(Unit)
        composeTestRule.waitForIdle()
        assertNotNull(actualRouteStops)
    }

    @Test
    fun testErrorCase() {
        val schedulesRepo = MockRouteStopsRepository(ApiResult.Error(500, "oops"))

        val errorRepo = MockErrorBannerStateRepository()

        composeTestRule.setContent { getRouteStops("", 0, "errorKey", schedulesRepo, errorRepo) }

        composeTestRule.waitUntil {
            when (val errorState = errorRepo.state.value) {
                is ErrorBannerState.DataError -> errorState.messages == setOf("errorKey")
                else -> false
            }
        }
    }
}
