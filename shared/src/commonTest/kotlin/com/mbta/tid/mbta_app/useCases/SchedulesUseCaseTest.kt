package com.mbta.tid.mbta_app

import SchedulesUseCase
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.koin.test.KoinTest

class SchedulesUseCaseTest : KoinTest {

    @Test
    fun testGetSchedule() {

        val scheduleResponse =
            ScheduleResponse(
                schedules =
                    listOf(
                        Schedule(
                            id = "sched1",
                            arrivalTime = Instant.parse("2024-01-02T03:04:05.00Z"),
                            departureTime = Instant.parse("2024-01-02T03:04:06.00Z"),
                            dropOffType = Schedule.StopEdgeType.Regular,
                            pickUpType = Schedule.StopEdgeType.Regular,
                            stopSequence = 0,
                            routeId = "Red",
                            stopId = "70064",
                            tripId = "trip1"
                        )
                    ),
                trips =
                    mapOf(
                        "trip1" to
                            Trip(
                                id = "trip1",
                                directionId = 0,
                                headsign = "Alewife",
                                routePatternId = "rp1",
                                shapeId = "shape1",
                                stopIds = listOf("70064", "70065")
                            )
                    )
            )

        class MockRepository() : ISchedulesRepository {
            override suspend fun getSchedule(
                stopIds: List<String>,
                now: Instant
            ): ScheduleResponse {
                return scheduleResponse
            }
        }

        runBlocking {
            assertEquals(
                scheduleResponse,
                SchedulesUseCase(repository = MockRepository())
                    .getSchedule(stopIds = listOf("place-davis"))
            )
        }
    }
}
