package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetScheduleTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSchedule() = runTest {
        fun buildSomeSchedules(): ScheduleResponse {
            val objects = ObjectCollectionBuilder()
            objects.schedule()
            objects.schedule()
            return ScheduleResponse(objects)
        }
        val expectedSchedules1 = buildSomeSchedules()
        val expectedSchedules2 = buildSomeSchedules()

        val stops1 = listOf("stop-a")
        val stops2 = listOf("stop-b")

        val schedulesRepo =
            object : ISchedulesRepository {
                override suspend fun getSchedule(
                    stopIds: List<String>,
                    now: Instant
                ): ApiResult<ScheduleResponse> {
                    return if (stopIds == stops1) ApiResult.Ok(expectedSchedules1)
                    else ApiResult.Ok(expectedSchedules2)
                }

                override suspend fun getSchedule(
                    stopIds: List<String>
                ): ApiResult<ScheduleResponse> {
                    throw IllegalStateException("Can't getSchedule with no time in this mock")
                }
            }

        var stopIds by mutableStateOf(stops1)
        var actualSchedules: ScheduleResponse? = expectedSchedules1
        composeTestRule.setContent {
            actualSchedules = getSchedule(stopIds = stopIds, "errorKey", schedulesRepo)
        }

        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules1, actualSchedules)

        stopIds = stops2
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules2, actualSchedules)
    }

    @Test
    fun testScheduleNoStops() {
        val schedulesRepo =
            MockScheduleRepository(
                callback = { stopIds -> assertEquals(emptyList<String>(), stopIds) }
            )
        var actualSchedules: ScheduleResponse? = null

        composeTestRule.setContent {
            actualSchedules = getSchedule(stopIds = emptyList(), "errorKey", schedulesRepo)
        }

        composeTestRule.waitUntil { actualSchedules != null }
        assertNotNull(actualSchedules)
    }

    @Test
    fun testErrorCase() {
        val schedulesRepo = MockScheduleRepository(ApiResult.Error(500, "oops"))

        val errorRepo = MockErrorBannerStateRepository()

        composeTestRule.setContent {
            getSchedule(stopIds = listOf("stop1"), "errorKey", schedulesRepo, errorRepo)
        }

        composeTestRule.waitUntil {
            when (val errorState = errorRepo.state.value) {
                is ErrorBannerState.DataError -> errorState.messages == setOf("errorKey")
                else -> false
            }
        }
    }
}
