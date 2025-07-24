package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.NewRouteStopsResult
import com.mbta.tid.mbta_app.repositories.OldRouteStopsResult
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
        val objects = ObjectCollectionBuilder()
        val route = objects.route {}
        fun buildSomeRouteStops(directionId: Int): NewRouteStopsResult {
            return NewRouteStopsResult(
                route.id,
                directionId,
                listOf(RouteBranchSegment.of(listOf(objects.stop().id, objects.stop().id))),
            )
        }
        val expectedRouteStops1 = buildSomeRouteStops(0)
        val expectedRouteStops2 = buildSomeRouteStops(1)

        val routeStopsRepo =
            object : IRouteStopsRepository {
                override suspend fun getOldRouteStops(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<OldRouteStopsResult> {
                    TODO("Not yet implemented")
                }

                override suspend fun getNewRouteSegments(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<NewRouteStopsResult> {
                    return if (directionId == 0) ApiResult.Ok(expectedRouteStops1)
                    else ApiResult.Ok(expectedRouteStops2)
                }
            }

        var directionId by mutableIntStateOf(0)
        var actualRouteStops: NewRouteStopsResult? = expectedRouteStops1
        composeTestRule.setContent {
            actualRouteStops = getRouteStops(route.id, directionId, "errorKey", routeStopsRepo)
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
                override suspend fun getOldRouteStops(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<OldRouteStopsResult> {
                    TODO("Not yet implemented")
                }

                override suspend fun getNewRouteSegments(
                    routeId: String,
                    directionId: Int,
                ): ApiResult<NewRouteStopsResult> {
                    sync.receive()
                    return ApiResult.Ok(NewRouteStopsResult(routeId, directionId, emptyList()))
                }
            }
        var actualRouteStops: NewRouteStopsResult? = null

        var directionId by mutableIntStateOf(0)
        composeTestRule.setContent {
            actualRouteStops = getRouteStops("", directionId, "errorKey", routeStopsRepo)
        }

        sync.send(Unit)
        composeTestRule.waitUntilDefaultTimeout { actualRouteStops != null }
        assertNotNull(actualRouteStops)

        directionId = 1
        composeTestRule.waitUntilDefaultTimeout { actualRouteStops == null }
        assertNull(actualRouteStops)

        sync.send(Unit)
        composeTestRule.waitUntilDefaultTimeout { actualRouteStops != null }
        assertNotNull(actualRouteStops)
    }

    @Test
    fun testErrorCase() {
        val schedulesRepo = MockRouteStopsRepository(ApiResult.Error(500, "oops"), null)

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
