package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.cache.MockKeyedCache
import com.mbta.tid.mbta_app.cache.ScheduleCache
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest

class SchedulesRepositoryTest : KoinTest {

    @AfterTest fun `stop koin`() = run { stopKoin() }

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
    }

    @Test
    fun `gets next schedule`() = runBlocking {
        val now = EasternTimeInstant.now()
        val routeId = "Red"
        val stopId = "place-davis"
        val directionId = 1
        val mockEngine = MockEngine { request ->
            println(request.url.encodedPathAndQuery)
            assertEquals("/api/schedules/next", request.url.encodedPath)
            assertEquals(
                parameters {
                    append("route", routeId)
                    append("stop", stopId)
                    append("direction", directionId.toString())
                    append("date_time", now.toString())
                },
                request.url.parameters,
            )
            respond(
                content =
                    ByteReadChannel(
                        """
                        {
                          "next_schedule": {
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
        val response =
            SchedulesRepository()
                .getNextSchedule(
                    LineOrRoute.Route(ObjectCollectionBuilder.Single.route { id = routeId }),
                    stopId = stopId,
                    directionId = directionId,
                    now = now,
                )
        assertEquals(
            ApiResult.Ok(
                NextScheduleResponse(
                    Schedule(
                        id = "sched1",
                        arrivalTime = EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 5),
                        departureTime = EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 6),
                        dropOffType = Schedule.StopEdgeType.Regular,
                        pickUpType = Schedule.StopEdgeType.Regular,
                        stopHeadsign = "Stop Headsign",
                        stopSequence = 0,
                        routeId = Route.Id("Red"),
                        stopId = "70064",
                        tripId = "trip1",
                    )
                )
            ),
            response,
        )
    }

    @Test
    fun testGetPartialCachedSchedule() = runTest {
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

        val objects = ObjectCollectionBuilder()
        val cachedTrip = objects.trip {}
        val cachedSchedule = objects.schedule { tripId = cachedTrip.id }
        val serviceDate = EasternTimeInstant.now().serviceDate

        val cacheMap =
            mutableMapOf(
                cachedSchedule.stopId to
                    ScheduleCache.Entry(
                        ScheduleResponse(
                            listOf(cachedSchedule),
                            mapOf(cachedTrip.id to cachedTrip),
                        ),
                        serviceDate,
                        cachedSchedule.stopId,
                    )
            )

        startKoin {
            modules(
                module {
                    single { MobileBackendClient(mockEngine, AppVariant.Staging) }
                    single { ScheduleCache(MockKeyedCache(cacheMap)) }
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) {
                        StandardTestDispatcher(testScheduler)
                    }
                }
            )
        }

        val cachedRepository = CachedSchedulesRepository(SchedulesRepository())

        val loadedTrip =
            Trip(
                id = "trip1",
                directionId = 0,
                headsign = "Alewife",
                routeId = Route.Id("Red"),
                routePatternId = "rp1",
                shapeId = "shape1",
                stopIds = listOf("70064", "70065"),
            )
        val loadedSchedule =
            Schedule(
                id = "sched1",
                arrivalTime = EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 5),
                departureTime = EasternTimeInstant(2024, Month.JANUARY, 2, 3, 4, 6),
                dropOffType = Schedule.StopEdgeType.Regular,
                pickUpType = Schedule.StopEdgeType.Regular,
                stopHeadsign = "Stop Headsign",
                stopSequence = 0,
                routeId = Route.Id("Red"),
                stopId = "70064",
                tripId = loadedTrip.id,
            )

        val response =
            cachedRepository.getSchedule(
                stopIds = listOf(loadedSchedule.stopId, cachedSchedule.stopId)
            )

        assertEquals(
            ApiResult.Ok(
                ScheduleResponse(
                    schedules = listOf(cachedSchedule, loadedSchedule),
                    trips = mapOf(cachedTrip.id to cachedTrip, loadedTrip.id to loadedTrip),
                )
            ),
            response,
        )
        testScheduler.advanceUntilIdle()
        val cachedResponse = cacheMap[loadedSchedule.stopId]!!
        assertEquals(loadedSchedule, cachedResponse.response.schedules.first())
        assertEquals(loadedTrip, cachedResponse.response.trips[loadedTrip.id])
        stopKoin()
    }

    @Test
    fun testGetPurelyCachedSchedules() = runBlocking {
        val mockEngine = MockEngine { fail("Endpoint should not be hit") }

        val objects = ObjectCollectionBuilder()
        val cachedTrip = objects.trip {}
        val cachedSchedule = objects.schedule { tripId = cachedTrip.id }
        val serviceDate = EasternTimeInstant.now().serviceDate

        startKoin {
            modules(
                module {
                    single { MobileBackendClient(mockEngine, AppVariant.Staging) }
                    single {
                        ScheduleCache(
                            MockKeyedCache(
                                mutableMapOf(
                                    cachedSchedule.stopId to
                                        ScheduleCache.Entry(
                                            ScheduleResponse(
                                                listOf(cachedSchedule),
                                                mapOf(cachedTrip.id to cachedTrip),
                                            ),
                                            serviceDate,
                                            cachedSchedule.stopId,
                                        )
                                )
                            )
                        )
                    }
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) { Dispatchers.IO }
                }
            )
        }

        val cachedRepository = CachedSchedulesRepository(SchedulesRepository())

        val response = cachedRepository.getSchedule(stopIds = listOf(cachedSchedule.stopId))

        assertEquals(
            ApiResult.Ok(
                ScheduleResponse(
                    schedules = listOf(cachedSchedule),
                    trips = mapOf(cachedTrip.id to cachedTrip),
                )
            ),
            response,
        )
        stopKoin()
    }

    @Test
    fun testCachedRepoErrorsWhenRequestFails() = runTest {
        val mockEngine = MockEngine { respondError(HttpStatusCode.BadGateway) }

        startKoin {
            modules(
                module {
                    single { MobileBackendClient(mockEngine, AppVariant.Staging) }
                    single { ScheduleCache(MockKeyedCache()) }
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) {
                        StandardTestDispatcher(testScheduler)
                    }
                }
            )
        }

        val cachedRepository = CachedSchedulesRepository(SchedulesRepository())

        val response = cachedRepository.getSchedule(stopIds = listOf("stopId"))

        assertEquals(HttpStatusCode.BadGateway.value, (response as? ApiResult.Error)?.code)
        stopKoin()
    }
}
