package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.SearchResponse
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
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
                                "type": "route"
                              },
                              "sort_order": 505200020,
                              "direction_id": 0,
                              "representative_trip": {
                                "id": "trip1",
                                "headsign": "Watertown",
                                "stops": null,
                                "route_pattern": null
                              }
                            },
                            "52-4-1": {
                              "id": "52-4-1",
                              "name": "Charles River Loop - Watertown via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "type": "route"
                              },
                              "sort_order": 505201010,
                              "direction_id": 1,
                              "representative_trip": {
                                "id": "trip2",
                                "headsign": "Charles River Loop"
                              }
                            },
                            "52-5-0": {
                              "id": "52-5-0",
                              "name": "Watertown - Dedham Mall via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "type": "route"
                              },
                              "sort_order": 505200000,
                              "direction_id": 0,
                              "representative_trip": {
                                "id": "trip3",
                                "headsign": "Watertown"
                              }
                            },
                            "52-5-1": {
                              "id": "52-5-1",
                              "name": "Dedham Mall - Watertown via Meadowbrook Rd",
                              "route": {
                                "id": "52",
                                "type": "route"
                              },
                              "sort_order": 505201000,
                              "direction_id": 1,
                              "representative_trip": {
                                "id": "trip4",
                                "headsign": "Dedham Mall"
                              }
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
                          },
                          "routes": {
                            "52": {
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
                            }
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
                StopAndRoutePatternResponse(
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
                                    representativeTrip = Trip(id = "trip1", headsign = "Watertown"),
                                    routeId = route52.id
                                ),
                            "52-4-1" to
                                RoutePattern(
                                    id = "52-4-1",
                                    directionId = 1,
                                    name = "Charles River Loop - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201010,
                                    representativeTrip =
                                        Trip(id = "trip2", headsign = "Charles River Loop"),
                                    routeId = route52.id
                                ),
                            "52-5-0" to
                                RoutePattern(
                                    id = "52-5-0",
                                    directionId = 0,
                                    name = "Watertown - Dedham Mall via Meadowbrook Rd",
                                    sortOrder = 505200000,
                                    representativeTrip = Trip(id = "trip3", headsign = "Watertown"),
                                    routeId = route52.id
                                ),
                            "52-5-1" to
                                RoutePattern(
                                    id = "52-5-1",
                                    directionId = 1,
                                    name = "Dedham Mall - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201000,
                                    representativeTrip =
                                        Trip(id = "trip4", headsign = "Dedham Mall"),
                                    routeId = route52.id
                                )
                        ),
                    patternIdsByStop =
                        mapOf(
                            "8552" to listOf("52-5-0", "52-4-0"),
                            "84791" to listOf("52-5-1", "52-4-1")
                        ),
                    routes = mapOf("52" to route52)
                ),
                response
            )
        }
    }

    @Test
    fun testGetSearchResults() {
        runBlocking {
            val mockEngine = MockEngine { request ->
                assertEquals("query=hay", request.url.encodedQuery)
                respond(
                    content =
                        ByteReadChannel(
                            """
                        {
                          "data": {
                            "routes": [
                              {
                                "id": "428",
                                "name": "428",
                                "type": "route",
                                "long_name": "Oaklandvale - Haymarket Station",
                                "route_type": 3,
                                "rank": 5
                              },
                              {
                                "id": "111",
                                "name": "111",
                                "type": "route",
                                "long_name": "Woodlawn - Haymarket Station",
                                "route_type": 3,
                                "rank": 5
                              },
                              {
                                "id": "426",
                                "name": "426",
                                "type": "route",
                                "long_name": "Central Square, Lynn - Haymarket or Wonderland Station",
                                "route_type": 3,
                                "rank": 5
                              },
                              {
                                "id": "450",
                                "name": "450",
                                "type": "route",
                                "long_name": "Salem Depot - Wonderland or Haymarket Station",
                                "route_type": 3,
                                "rank": 5
                              },
                              {
                                "id": "Green-D",
                                "name": "Green Line D",
                                "type": "route",
                                "long_name": "Green Line D",
                                "route_type": 0,
                                "rank": 2
                              }
                            ],
                            "stops": [
                              {
                                "id": "place-haecl",
                                "name": "Haymarket",
                                "type": "stop",
                                "routes": [
                                  {
                                    "type": 1,
                                    "icon": "orange_line"
                                  },
                                  {
                                    "type": 0,
                                    "icon": "green_line_d"
                                  },
                                  {
                                    "type": 0,
                                    "icon": "green_line_e"
                                  }
                                ],
                                "rank": 2,
                                "zone": null,
                                "station?": true
                              },
                              {
                                "id": "78741",
                                "name": "Worthen Rd @ Hayden Rec Ctr",
                                "type": "stop",
                                "routes": [
                                  {
                                    "type": 3,
                                    "icon": "bus"
                                  }
                                ],
                                "rank": 5,
                                "zone": null,
                                "station?": false
                              }
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
            val response = backend.getSearchResults("hay")

            assertEquals(
                SearchResponse(
                    data =
                        SearchResults(
                            routes =
                                listOf(
                                    RouteResult(
                                        id = "428",
                                        shortName = "428",
                                        longName = "Oaklandvale - Haymarket Station",
                                        routeType = RouteType.BUS,
                                        rank = 5
                                    ),
                                    RouteResult(
                                        id = "111",
                                        shortName = "111",
                                        longName = "Woodlawn - Haymarket Station",
                                        routeType = RouteType.BUS,
                                        rank = 5
                                    ),
                                    RouteResult(
                                        id = "426",
                                        shortName = "426",
                                        longName =
                                            "Central Square, Lynn - Haymarket or Wonderland Station",
                                        routeType = RouteType.BUS,
                                        rank = 5
                                    ),
                                    RouteResult(
                                        id = "450",
                                        shortName = "450",
                                        longName = "Salem Depot - Wonderland or Haymarket Station",
                                        routeType = RouteType.BUS,
                                        rank = 5
                                    ),
                                    RouteResult(
                                        id = "Green-D",
                                        shortName = "Green Line D",
                                        longName = "Green Line D",
                                        routeType = RouteType.TRAM,
                                        rank = 2
                                    )
                                ),
                            stops =
                                listOf(
                                    StopResult(
                                        id = "place-haecl",
                                        name = "Haymarket",
                                        routes =
                                            listOf(
                                                StopResultRoute(
                                                    type = RouteType.SUBWAY,
                                                    icon = "orange_line"
                                                ),
                                                StopResultRoute(
                                                    type = RouteType.TRAM,
                                                    icon = "green_line_d"
                                                ),
                                                StopResultRoute(
                                                    type = RouteType.TRAM,
                                                    icon = "green_line_e"
                                                ),
                                            ),
                                        rank = 2,
                                        zone = null,
                                        isStation = true
                                    ),
                                    StopResult(
                                        id = "78741",
                                        name = "Worthen Rd @ Hayden Rec Ctr",
                                        routes =
                                            listOf(
                                                StopResultRoute(type = RouteType.BUS, icon = "bus")
                                            ),
                                        rank = 5,
                                        zone = null,
                                        isStation = false
                                    )
                                )
                        )
                ),
                response
            )
        }
    }
}
