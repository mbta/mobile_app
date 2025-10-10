package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.TripShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class TripRepositoryTest : KoinTest {
    @Test
    fun `gets trip schedules`() {
        val mockEngine = MockEngine {
            respond(
                """{"type": "stop_ids", "stop_ids": ["1", "2", "3"]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }

        runBlocking {
            val response = TripRepository().getTripSchedules(tripId = "12345")

            assertEquals(
                ApiResult.Ok(TripSchedulesResponse.StopIds(listOf("1", "2", "3"))),
                response,
            )
        }

        stopKoin()
    }

    @Test
    fun `gets trip shape when shaped found`() {
        val mockEngine = MockEngine {
            respond(
                """
                  {
                      "shape_with_stops": {
                          "shape": {"id": "shape_id", "polyline": "shape_polyline"},
                          "stop_ids": ["1", "2", "3"],
                          "route_id": "66",
                          "route_pattern_id": "66_rp",
                          "direction_id": 1
                      }
                    }
                """
                    .trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }

        runBlocking {
            val response = TripRepository().getTripShape(tripId = "12345")

            assertEquals(
                ApiResult.Ok(
                    TripShape(
                        shapeWithStops =
                            ShapeWithStops(
                                directionId = 1,
                                routeId = Route.Id("66"),
                                routePatternId = "66_rp",
                                shape = Shape(id = "shape_id", polyline = "shape_polyline"),
                                stopIds = listOf("1", "2", "3"),
                            )
                    )
                ),
                response,
            )
        }

        stopKoin()
    }

    @Test
    fun `getsTripShape error response when not found`() {
        val mockEngine = MockEngine {
            respond(
                """
                  {
                      "code": 404,
                      "message": "not_found"
                    }
                """
                    .trimIndent(),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }

        runBlocking {
            val apiResult = TripRepository().getTripShape(tripId = "12345")

            assertIs<ApiResult.Error<*>>(apiResult)
            assertEquals(404, apiResult.code)
            assertContains(apiResult.message, "not_found")
        }

        stopKoin()
    }

    @Test
    fun `getsTripShape when parsing error`() {
        val mockEngine = MockEngine {
            respond(
                """
                {"field": "Can't parse me!"}
                """
                    .trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }

        runBlocking {
            val apiResult = TripRepository().getTripShape(tripId = "12345")
            assertIs<ApiResult.Error<*>>(apiResult)

            assertContains(apiResult.message, "Field 'shape_with_stops' is required for type")
        }

        stopKoin()
    }
}
