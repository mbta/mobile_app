package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePatternKey
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
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
                    "map_friendly_route_shapes": [
                        {
                          "route_id": "66",
                          "route_shapes": [
                            {
                              "direction_id": 1,
                              "shape": {
                                "id": "shape_id",
                                "polyline": "shape_polyline"
                              },
                              "source_route_pattern_id": "66_rp",
                              "source_route_id": "66",
                              "route_segments": [
                                {
                                  "id": "1-3",
                                  "stop_ids": ["1","2","3"],
                                  "source_route_pattern_id": "66_rp",
                                  "source_route_id": "66",
                                  "other_patterns_by_stop_id": {
                                    "1": [
                                      { "route_id": "66", "route_pattern_id": "66_rp_2" }
                                    ],
                                    "2": [
                                      { "route_id": "66", "route_pattern_id": "66_rp_2" }
                                    ],
                                    "3": [
                                      { "route_id": "66", "route_pattern_id": "66_rp_2" }
                                    ]
                                  }
                                }
                              ]
                            }
                          ]
                        }
                      ]
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
            val routeId = Route.Id("66")

            assertEquals(
                ApiResult.Ok(
                    MapFriendlyRouteResponse(
                        routesWithSegmentedShapes =
                            listOf(
                                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                                    routeId = routeId,
                                    segmentedShapes =
                                        listOf(
                                            SegmentedRouteShape(
                                                sourceRoutePatternId = "66_rp",
                                                sourceRouteId = routeId,
                                                directionId = 1,
                                                routeSegments =
                                                    listOf(
                                                        RouteSegment(
                                                            id = "1-3",
                                                            sourceRoutePatternId = "66_rp",
                                                            sourceRouteId = routeId,
                                                            stopIds = listOf("1", "2", "3"),
                                                            otherPatternsByStopId =
                                                                mapOf(
                                                                    "1" to
                                                                        listOf(
                                                                            RoutePatternKey(
                                                                                routeId,
                                                                                "66_rp_2",
                                                                            )
                                                                        ),
                                                                    "2" to
                                                                        listOf(
                                                                            RoutePatternKey(
                                                                                routeId,
                                                                                "66_rp_2",
                                                                            )
                                                                        ),
                                                                    "3" to
                                                                        listOf(
                                                                            RoutePatternKey(
                                                                                routeId,
                                                                                "66_rp_2",
                                                                            )
                                                                        ),
                                                                ),
                                                        )
                                                    ),
                                                shape =
                                                    Shape(
                                                        id = "shape_id",
                                                        polyline = "shape_polyline",
                                                    ),
                                            )
                                        ),
                                )
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

            assertContains(
                apiResult.message,
                "Field 'map_friendly_route_shapes' is required for type",
            )
        }

        stopKoin()
    }
}
