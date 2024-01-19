package com.mbta.tid.mbta_app

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class BackendTest {
    @Test
    fun testGetNearby() {
        runBlocking {
            val mockEngine = MockEngine { request ->
                assertEquals("latitude=12.34&longitude=-56.78", request.url.encodedQuery)
                respond(
                    content =
                        ByteReadChannel(
                            """
                        {
                          "stops": [
                            {
                              "id": "8552",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289904,
                              "longitude": -71.191003,
                              "parent_station": null
                            },
                            {
                              "id": "84791",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289995,
                              "longitude": -71.191092,
                              "parent_station": null
                            }
                          ],
                          "route_patterns": {
                            "52-4-0": {
                              "id": "52-4-0",
                              "name": "Watertown - Charles River Loop via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "color": "FFC72C",
                                "direction_names": [
                                  "Outbound",
                                  "Inbound"
                                ],
                                "direction_destinations": [
                                  "Dedham Mall",
                                  "Watertown Yard"
                                ],
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                              },
                              "sort_order": 505200020,
                              "direction_id": 0,
                              "representative_trip": null
                            },
                            "52-4-1": {
                              "id": "52-4-1",
                              "name": "Charles River Loop - Watertown via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "color": "FFC72C",
                                "direction_names": [
                                  "Outbound",
                                  "Inbound"
                                ],
                                "direction_destinations": [
                                  "Dedham Mall",
                                  "Watertown Yard"
                                ],
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                              },
                              "sort_order": 505201010,
                              "direction_id": 1,
                              "representative_trip": null
                            },
                            "52-5-0": {
                              "id": "52-5-0",
                              "name": "Watertown - Dedham Mall via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "color": "FFC72C",
                                "direction_names": [
                                  "Outbound",
                                  "Inbound"
                                ],
                                "direction_destinations": [
                                  "Dedham Mall",
                                  "Watertown Yard"
                                ],
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                              },
                              "sort_order": 505200000,
                              "direction_id": 0,
                              "representative_trip": null
                            },
                            "52-5-1": {
                              "id": "52-5-1",
                              "name": "Dedham Mall - Watertown via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "color": "FFC72C",
                                "direction_names": [
                                  "Outbound",
                                  "Inbound"
                                ],
                                "direction_destinations": [
                                  "Dedham Mall",
                                  "Watertown Yard"
                                ],
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                              },
                              "sort_order": 505201000,
                              "direction_id": 1,
                              "representative_trip": null
                            }
                          },
                          "pattern_ids_by_stop": {
                            "8552": [
                              "52-5-0",
                              "52-4-0"
                            ],
                            "84791": [
                              "52-5-1",
                              "52-4-1"
                            ]
                          }
                        }
                    """
                                .trimIndent()
                        ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val backend = Backend(mockEngine)
            val response = backend.getNearby(12.34, -56.78)

            val route52 =
                Route(
                    id = "52",
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Dedham Mall", "Watertown Yard"),
                    longName = "Dedham Mall - Watertown Yard",
                    shortName = "52",
                    sortOrder = 50520,
                    textColor = "000000"
                )
            assertEquals(
                NearbyResponse(
                    stops =
                        listOf(
                            Stop(
                                id = "8552",
                                latitude = 42.289904,
                                longitude = -71.191003,
                                name = "Sawmill Brook Pkwy @ Walsh Rd",
                                parentStation = null
                            ),
                            Stop(
                                id = "84791",
                                latitude = 42.289995,
                                longitude = -71.191092,
                                name = "Sawmill Brook Pkwy @ Walsh Rd",
                                parentStation = null
                            )
                        ),
                    routePatterns =
                        mapOf(
                            "52-4-0" to
                                RoutePattern(
                                    id = "52-4-0",
                                    directionId = 0,
                                    name = "Watertown - Charles River Loop via Meadowbrook Rd",
                                    sortOrder = 505200020,
                                    route = route52
                                ),
                            "52-4-1" to
                                RoutePattern(
                                    id = "52-4-1",
                                    directionId = 1,
                                    name = "Charles River Loop - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201010,
                                    route = route52
                                ),
                            "52-5-0" to
                                RoutePattern(
                                    id = "52-5-0",
                                    directionId = 0,
                                    name = "Watertown - Dedham Mall via Meadowbrook Rd",
                                    sortOrder = 505200000,
                                    route = route52
                                ),
                            "52-5-1" to
                                RoutePattern(
                                    id = "52-5-1",
                                    directionId = 1,
                                    name = "Dedham Mall - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201000,
                                    route = route52
                                )
                        ),
                    patternIdsByStop =
                        mapOf(
                            "8552" to listOf("52-5-0", "52-4-0"),
                            "84791" to listOf("52-5-1", "52-4-1")
                        )
                ),
                response
            )
        }
    }
}
