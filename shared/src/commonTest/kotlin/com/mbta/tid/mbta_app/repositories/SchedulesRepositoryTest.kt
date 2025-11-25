package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Month
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class SchedulesRepositoryTest : KoinTest {

    @Test
    fun testGetSchedule() {
        val mockEngine = MockEngine { request ->
            respond(
                content =
                    ByteReadChannel(
                        """
                        {
                          "schedules": [{
                            "id": "sched1",
                            "arrival_time": "2024-01-02T03:04:05.00-05:00",
                            "departure_time": "2024-01-02T03:04:06.00-05:00",
                            "drop_off_type": "regular",
                            "pick_up_type":  "regular",
                            "stop_headsign": "Stop Headsign",
                            "stop_sequence": 0,
                            "route_id": "Red",
                            "stop_id": "70064",
                            "trip_id": "trip1"
                            }
                          ],
                          "trips": {
                            "trip1": {
                              "id": "trip1",
                              "direction_id": 0,
                              "headsign": "Alewife",
                              "route_id": "Red",
                              "route_pattern_id": "rp1",
                              "shape_id": "shape1",
                              "stop_ids": ["70064", "70065"]
                            }
                          }
                        }
                        """
                            .trimIndent()
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }
        runBlocking {
            val response = SchedulesRepository().getSchedule(stopIds = listOf("place-davis"))
            assertEquals(
                ApiResult.Ok(
                    ScheduleResponse(
                        schedules =
                            listOf(
                                Schedule(
                                    id = "sched1",
                                    arrivalTime =
                                        EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 5),
                                    departureTime =
                                        EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 6),
                                    dropOffType = Schedule.StopEdgeType.Regular,
                                    pickUpType = Schedule.StopEdgeType.Regular,
                                    stopHeadsign = "Stop Headsign",
                                    stopSequence = 0,
                                    routeId = Route.Id("Red"),
                                    stopId = "70064",
                                    tripId = "trip1",
                                )
                            ),
                        trips =
                            mapOf(
                                "trip1" to
                                    Trip(
                                        id = "trip1",
                                        directionId = 0,
                                        headsign = "Alewife",
                                        routeId = Route.Id("Red"),
                                        routePatternId = "rp1",
                                        shapeId = "shape1",
                                        stopIds = listOf("70064", "70065"),
                                    )
                            ),
                    )
                ),
                response,
            )
        }
        stopKoin()
    }
}
