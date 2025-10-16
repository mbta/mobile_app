package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.StopRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class StopRepositoryTest : KoinTest {
    @Test
    fun testGetStopData() {
        val mockEngine = MockEngine { _ ->
            respond(
                content =
                    ByteReadChannel(
                        """
                        {
                          "map_friendly_route_shapes": [
                            {
                              "route_id": "Blue",
                              "route_shapes": [
                                {
                                  "source_route_pattern_id": "Blue-6-0",
                                  "source_route_id": "Blue",
                                  "direction_id": 0,
                                  "route_segments": [
                                    {
                                      "id": "place-wondl-place-bomnl",
                                      "source_route_id": "Blue",
                                      "stop_ids": [
                                        "place-wondl",
                                        "place-bomnl"
                                      ],
                                      "other_patterns_by_stop_id": {},
                                      "source_route_pattern_id": "Blue-6-0"
                                    }
                                  ],
                                  "shape": {
                                    "id": "canonical-946_0013",
                                    "polyline": "s|zaG~phpLpBwO"
                                  }
                                }
                              ]
                            }
                          ],
                          "child_stops": {
                            "70060": {
                              "id": "70060",
                              "name": "Wonderland",
                              "description": "Wonderland - Blue Line - Exit Only",
                              "location_type": "stop",
                              "latitude": 42.413361,
                              "longitude": -70.991685,
                              "vehicle_type": "heavy_rail",
                              "platform_name": "Exit Only",
                              "parent_station_id": "place-wondl"
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
            val response = StopRepository().getStopMapData(stopId = "place-wondl")
            assertEquals(
                ApiResult.Ok(
                    StopMapResponse(
                        routeShapes =
                            listOf(
                                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                                    routeId = Route.Id("Blue"),
                                    segmentedShapes =
                                        listOf(
                                            SegmentedRouteShape(
                                                sourceRoutePatternId = "Blue-6-0",
                                                sourceRouteId = Route.Id("Blue"),
                                                directionId = 0,
                                                routeSegments =
                                                    listOf(
                                                        RouteSegment(
                                                            id = "place-wondl-place-bomnl",
                                                            sourceRouteId = Route.Id("Blue"),
                                                            stopIds =
                                                                listOf(
                                                                    "place-wondl",
                                                                    "place-bomnl",
                                                                ),
                                                            otherPatternsByStopId = mapOf(),
                                                            sourceRoutePatternId = "Blue-6-0",
                                                        )
                                                    ),
                                                shape =
                                                    Shape(
                                                        id = "canonical-946_0013",
                                                        polyline = "s|zaG~phpLpBwO",
                                                    ),
                                            )
                                        ),
                                )
                            ),
                        childStops =
                            mapOf(
                                Pair(
                                    "70060",
                                    Stop(
                                        id = "70060",
                                        name = "Wonderland",
                                        description = "Wonderland - Blue Line - Exit Only",
                                        locationType = LocationType.STOP,
                                        latitude = 42.413361,
                                        longitude = -70.991685,
                                        platformName = "Exit Only",
                                        vehicleType = RouteType.HEAVY_RAIL,
                                        parentStationId = "place-wondl",
                                    ),
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
