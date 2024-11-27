package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val expectedSchedules3 = buildSomeSchedules()

        val stops1 = listOf("stop-a")
        val stops2 = listOf("stop-b")

        val time1 = Instant.parse("2024-08-12T10:15:14-06:00")
        val time2 = Instant.parse("2024-08-12T10:17:43-06:00")

        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        val schedulesRepo =
            object : ISchedulesRepository {
                override suspend fun getSchedule(
                    stopIds: List<String>,
                    now: Instant
                ): ApiResult<ScheduleResponse> {
                    requestSync.receive()
                    return if (stopIds == stops1 && now == time1) ApiResult.Ok(expectedSchedules1)
                    else if (stopIds == stops1) ApiResult.Ok(expectedSchedules2)
                    else ApiResult.Ok(expectedSchedules3)
                }

                override suspend fun getSchedule(
                    stopIds: List<String>
                ): ApiResult<ScheduleResponse> {
                    throw IllegalStateException("Can't getSchedule with no time in this mock")
                }
            }

        var stopIds by mutableStateOf(stops1)
        var now by mutableStateOf(time1)
        var actualSchedules: ScheduleResponse? = expectedSchedules1
        composeTestRule.setContent {
            actualSchedules = getSchedule(stopIds = stopIds, schedulesRepo)
        }

        composeTestRule.awaitIdle()
        assertNull(actualSchedules)

        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules1, actualSchedules)

        now = time2
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules1, actualSchedules)
        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules2, actualSchedules)

        stopIds = stops2
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules2, actualSchedules)
        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules3, actualSchedules)
    }
}
