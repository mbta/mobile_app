package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.SearchResponse
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
    fun testGetGlobal() {
        runBlocking {
            val mockEngine = MockEngine { request ->
                respond(
                    content =
                        ByteReadChannel(
                            """
                        {
                          "stops": {
                            "8552": {
                              "id": "8552",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289904,
                              "longitude": -71.191003,
                              "location_type": "stop",
                              "parent_station": null
                            },
                            "84791": {
                              "id": "84791",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289995,
                              "longitude": -71.191092,
                              "location_type": "stop",
                              "parent_station": null
                            }
                          },
                          "route_patterns": {
                            "52-4-0": {
                              "id": "52-4-0",
                              "name": "Watertown - Charles River Loop via Meadowbrook Rd",
                              "route_id": "52"
                              "sort_order": 505200020,
                              "direction_id": 0,
                              "representative_trip_id": "trip1",
                              "typicality": "deviation"
                            },
                            "52-4-1": {
                              "id": "52-4-1",
                              "name": "Charles River Loop - Watertown via Meadowbrook Rd",
                              "route_id": "52"
                              "sort_order": 505201010,
                              "direction_id": 1,
                              "representative_trip_id": "trip2",
                              "typicality": "deviation"
                            },
                            "52-5-0": {
                              "id": "52-5-0",
                              "name": "Watertown - Dedham Mall via Meadowbrook Rd",
                              "route_id": "52"
                              "sort_order": 505200000,
                              "direction_id": 0,
                              "representative_trip_id": "trip3",
                              "typicality": "typical"
                            },
                            "52-5-1": {
                              "id": "52-5-1",
                              "name": "Dedham Mall - Watertown via Meadowbrook Rd",
                              "route_id": "52"
                              "sort_order": 505201000,
                              "direction_id": 1,
                              "representative_trip_id": "trip4",
                              "typicality": "typical"
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
                                "type": "bus",
                                "color": "FFC72C",
                                "direction_names": [
                                  "Outbound",
                                  "Inbound"
                                ],
                                "direction_destinations": [
                                  "Dedham Mall",
                                  "Watertown Yard"
                                ],
                                "line_id": "line-5259",
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                            }
                          },
                          "trips": {
                            "trip1": {
                              "id": "trip1",
                              "direction_id": 0,
                              "headsign": "Watertown",
                              "route_id": "52",
                              "route_pattern_id": "52-4-0",
                              "shape_id": "520215"
                            },
                            "trip2": {
                              "id": "trip2",
                              "direction_id": 1,
                              "headsign": "Charles River Loop",
                               "route_id": "52",
                              "route_pattern_id": "52-4-1",
                              "shape_id": "520213"
                            },
                            "trip3": {
                              "id": "trip3",
                              "direction_id": 0,
                              "headsign": "Watertown",
                              "route_id": "52",
                              "route_pattern_id": "52-5-0",
                              "shape_id": "520212"
                            },
                            "trip4": {
                              "id": "trip4",
                              "direction_id": 1,
                              "headsign": "Dedham Mall",
                              "route_id": "52",
                              "route_pattern_id": "52-5-1",
                              "shape_id": "520211"
                            }
                          },
                          "lines": {
                            "line-5259": {
                              "id": "line-5259",
                              "color": "FFC72C",
                              "long_name": "Needham Junction or Dedham Mall - Watertown",
                              "short_name": "52/59",
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

            val backend = Backend(mockEngine, AppVariant.Staging)
            val response = backend.getGlobalData()

            val route52 =
                Route(
                    id = "52",
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Dedham Mall", "Watertown Yard"),
                    longName = "Dedham Mall - Watertown Yard",
                    shortName = "52",
                    sortOrder = 50520,
                    textColor = "000000",
                    lineId = "line-5259"
                )
            assertEquals(
                GlobalResponse(
                    stops =
                        mapOf(
                            "8552" to
                                Stop(
                                    id = "8552",
                                    latitude = 42.289904,
                                    longitude = -71.191003,
                                    locationType = LocationType.STOP,
                                    name = "Sawmill Brook Pkwy @ Walsh Rd"
                                ),
                            "84791" to
                                Stop(
                                    id = "84791",
                                    latitude = 42.289995,
                                    longitude = -71.191092,
                                    locationType = LocationType.STOP,
                                    name = "Sawmill Brook Pkwy @ Walsh Rd"
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
                                    typicality = RoutePattern.Typicality.Deviation,
                                    representativeTripId = "trip1",
                                    routeId = route52.id
                                ),
                            "52-4-1" to
                                RoutePattern(
                                    id = "52-4-1",
                                    directionId = 1,
                                    name = "Charles River Loop - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201010,
                                    typicality = RoutePattern.Typicality.Deviation,
                                    representativeTripId = "trip2",
                                    routeId = route52.id
                                ),
                            "52-5-0" to
                                RoutePattern(
                                    id = "52-5-0",
                                    directionId = 0,
                                    name = "Watertown - Dedham Mall via Meadowbrook Rd",
                                    sortOrder = 505200000,
                                    typicality = RoutePattern.Typicality.Typical,
                                    representativeTripId = "trip3",
                                    routeId = route52.id
                                ),
                            "52-5-1" to
                                RoutePattern(
                                    id = "52-5-1",
                                    directionId = 1,
                                    name = "Dedham Mall - Watertown via Meadowbrook Rd",
                                    sortOrder = 505201000,
                                    typicality = RoutePattern.Typicality.Typical,
                                    representativeTripId = "trip4",
                                    routeId = route52.id
                                )
                        ),
                    patternIdsByStop =
                        mapOf(
                            "8552" to listOf("52-5-0", "52-4-0"),
                            "84791" to listOf("52-5-1", "52-4-1")
                        ),
                    routes = mapOf("52" to route52),
                    trips =
                        mapOf(
                            "trip1" to
                                Trip(
                                    id = "trip1",
                                    directionId = 0,
                                    headsign = "Watertown",
                                    routeId = "52",
                                    routePatternId = "52-4-0",
                                    shapeId = "520215"
                                ),
                            "trip2" to
                                Trip(
                                    id = "trip2",
                                    directionId = 1,
                                    headsign = "Charles River Loop",
                                    routeId = "52",
                                    routePatternId = "52-4-1",
                                    shapeId = "520213"
                                ),
                            "trip3" to
                                Trip(
                                    id = "trip3",
                                    directionId = 0,
                                    headsign = "Watertown",
                                    routeId = "52",
                                    routePatternId = "52-5-0",
                                    shapeId = "520212"
                                ),
                            "trip4" to
                                Trip(
                                    id = "trip4",
                                    directionId = 1,
                                    headsign = "Dedham Mall",
                                    routeId = "52",
                                    routePatternId = "52-5-1",
                                    shapeId = "520211"
                                )
                        ),
                    lines =
                        mapOf(
                            "line-5259" to
                                Line(
                                    id = "line-5259",
                                    color = "FFC72C",
                                    longName = "Needham Junction or Dedham Mall - Watertown",
                                    shortName = "52/59",
                                    sortOrder = 50520,
                                    textColor = "000000"
                                )
                        )
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
                                "route_type": "bus",
                                "rank": 5
                              },
                              {
                                "id": "111",
                                "name": "111",
                                "type": "route",
                                "long_name": "Woodlawn - Haymarket Station",
                                "route_type": "bus",
                                "rank": 5
                              },
                              {
                                "id": "426",
                                "name": "426",
                                "type": "route",
                                "long_name": "Central Square, Lynn - Haymarket or Wonderland Station",
                                "route_type": "bus",
                                "rank": 5
                              },
                              {
                                "id": "450",
                                "name": "450",
                                "type": "route",
                                "long_name": "Salem Depot - Wonderland or Haymarket Station",
                                "route_type": "bus",
                                "rank": 5
                              },
                              {
                                "id": "Green-D",
                                "name": "Green Line D",
                                "type": "route",
                                "long_name": "Green Line D",
                                "route_type": "light_rail",
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
                                    "type": "heavy_rail",
                                    "icon": "orange_line"
                                  },
                                  {
                                    "type": "light_rail",
                                    "icon": "green_line_d"
                                  },
                                  {
                                    "type": "light_rail",
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
                                    "type": "bus",
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

            val backend = Backend(mockEngine, AppVariant.Staging)
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
                                        routeType = RouteType.LIGHT_RAIL,
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
                                                    type = RouteType.HEAVY_RAIL,
                                                    icon = "orange_line"
                                                ),
                                                StopResultRoute(
                                                    type = RouteType.LIGHT_RAIL,
                                                    icon = "green_line_d"
                                                ),
                                                StopResultRoute(
                                                    type = RouteType.LIGHT_RAIL,
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

    @Test
    fun testGetMapFriendlyRailShapes() {
        runBlocking {
            val mockEngine = MockEngine { request ->
                assertEquals("", request.url.encodedQuery)
                respond(
                    content =
                        ByteReadChannel(
                            """
                  {
                    "map_friendly_route_shapes": [
                        {
                            "route_id": "Red",
                            "route_shapes": [
                                {
                                    "source_route_pattern_id": "red-ashmont",
                                    "source_route_id": "Red",
                                    "direction_id": 0,
                                                                "shape": {
                                                                    "id": "ashmont_shape",
                                                                    "polyline": "ashmont_shape_polyline"
                                                                }
                                    "route_segments": [
                                        {
                                            "id": "andrew-savin_hill",
                                            "source_route_id": "Red",
                                            "source_route_pattern_id": "red-ashmont",
                                            "stop_ids": [
                                                "andrew",
                                                "jfk/umass",
                                                "savin_hill"
                                            ],
                                            "other_patterns_by_stop_id": {}
                                        }
                                    ],
                                    "shape": {
                                        "id": "ashmont_shape",
                                        "polyline": "ashmont_shape_polyline"
                                    }
                                },
                                {
                                    "source_route_pattern_id": "red-braintree",
                                    "source_route_id": "Red",
                                    "direction_id": 0,
                                    "route_segments": [
                                        {
                                            "id": "jfk/umass-north_quincy",
                                            "source_route_id": "Red",
                                            "source_route_pattern_id": "red-braintree",
                                            "stop_ids": [
                                                "jfk/umass",
                                                "north_quincy"
                                            ],
                                            "other_patterns_by_stop_id": {}
                                        }
                                    ],
                                    "shape": {
                                        "id": "braintree_shape",
                                        "polyline": "braintree_shape_polyline"
                                    }
                                }
                            ]
                        }
                    ]
                }
                            """
                                .trimIndent()
                        ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val backend = Backend(mockEngine, AppVariant.Staging)
            val response = backend.getMapFriendlyRailShapes()

            assertEquals(
                MapFriendlyRouteResponse(
                    routesWithSegmentedShapes =
                        listOf(
                            MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                                routeId = "Red",
                                segmentedShapes =
                                    listOf(
                                        SegmentedRouteShape(
                                            sourceRouteId = "Red",
                                            sourceRoutePatternId = "red-ashmont",
                                            directionId = 0,
                                            shape =
                                                Shape(
                                                    id = "ashmont_shape",
                                                    polyline = "ashmont_shape_polyline"
                                                ),
                                            routeSegments =
                                                listOf(
                                                    RouteSegment(
                                                        id = "andrew-savin_hill",
                                                        sourceRoutePatternId = "red-ashmont",
                                                        sourceRouteId = "Red",
                                                        stopIds =
                                                            listOf(
                                                                "andrew",
                                                                "jfk/umass",
                                                                "savin_hill"
                                                            ),
                                                        otherPatternsByStopId = mapOf()
                                                    )
                                                )
                                        ),
                                        SegmentedRouteShape(
                                            sourceRouteId = "Red",
                                            sourceRoutePatternId = "red-braintree",
                                            directionId = 0,
                                            shape =
                                                Shape(
                                                    id = "braintree_shape",
                                                    polyline = "braintree_shape_polyline"
                                                ),
                                            routeSegments =
                                                listOf(
                                                    RouteSegment(
                                                        id = "jfk/umass-north_quincy",
                                                        sourceRoutePatternId = "red-braintree",
                                                        sourceRouteId = "Red",
                                                        stopIds =
                                                            listOf("jfk/umass", "north_quincy"),
                                                        otherPatternsByStopId = mapOf()
                                                    )
                                                )
                                        ),
                                    )
                            )
                        )
                ),
                response
            )
        }
    }
}
