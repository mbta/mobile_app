package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class RouteStopsRepositoryTest : KoinTest {
    @Test
    fun testGetNewSegments() {
        lateinit var requestUrl: Url
        val mockEngine = MockEngine { request ->
            requestUrl = request.url
            respond(
                content =
                    ByteReadChannel(
                        """
                        [
                          {
                            "name": null,
                            "stops": [
                              {
                                "stop_id": "place-ogmnl",
                                "connections": [
                                  {
                                    "from_stop": "place-ogmnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-mlmnl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-mlmnl",
                                "connections": [
                                  {
                                    "from_stop": "place-ogmnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-mlmnl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-mlmnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-welln",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-welln",
                                "connections": [
                                  {
                                    "from_stop": "place-mlmnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-welln",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-welln",
                                    "from_vpos": "center",
                                    "to_stop": "place-astao",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-astao",
                                "connections": [
                                  {
                                    "from_stop": "place-welln",
                                    "from_vpos": "top",
                                    "to_stop": "place-astao",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-astao",
                                    "from_vpos": "center",
                                    "to_stop": "place-sull",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-sull",
                                "connections": [
                                  {
                                    "from_stop": "place-astao",
                                    "from_vpos": "top",
                                    "to_stop": "place-sull",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-sull",
                                    "from_vpos": "center",
                                    "to_stop": "place-ccmnl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-ccmnl",
                                "connections": [
                                  {
                                    "from_stop": "place-sull",
                                    "from_vpos": "top",
                                    "to_stop": "place-ccmnl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-ccmnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-north",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-north",
                                "connections": [
                                  {
                                    "from_stop": "place-ccmnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-north",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-north",
                                    "from_vpos": "center",
                                    "to_stop": "place-haecl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-haecl",
                                "connections": [
                                  {
                                    "from_stop": "place-north",
                                    "from_vpos": "top",
                                    "to_stop": "place-haecl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-haecl",
                                    "from_vpos": "center",
                                    "to_stop": "place-state",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-state",
                                "connections": [
                                  {
                                    "from_stop": "place-haecl",
                                    "from_vpos": "top",
                                    "to_stop": "place-state",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-state",
                                    "from_vpos": "center",
                                    "to_stop": "place-dwnxg",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-dwnxg",
                                "connections": [
                                  {
                                    "from_stop": "place-state",
                                    "from_vpos": "top",
                                    "to_stop": "place-dwnxg",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-dwnxg",
                                    "from_vpos": "center",
                                    "to_stop": "place-chncl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-chncl",
                                "connections": [
                                  {
                                    "from_stop": "place-dwnxg",
                                    "from_vpos": "top",
                                    "to_stop": "place-chncl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-chncl",
                                    "from_vpos": "center",
                                    "to_stop": "place-tumnl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-tumnl",
                                "connections": [
                                  {
                                    "from_stop": "place-chncl",
                                    "from_vpos": "top",
                                    "to_stop": "place-tumnl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-tumnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-bbsta",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-bbsta",
                                "connections": [
                                  {
                                    "from_stop": "place-tumnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-bbsta",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-bbsta",
                                    "from_vpos": "center",
                                    "to_stop": "place-masta",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-masta",
                                "connections": [
                                  {
                                    "from_stop": "place-bbsta",
                                    "from_vpos": "top",
                                    "to_stop": "place-masta",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-masta",
                                    "from_vpos": "center",
                                    "to_stop": "place-rugg",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-rugg",
                                "connections": [
                                  {
                                    "from_stop": "place-masta",
                                    "from_vpos": "top",
                                    "to_stop": "place-rugg",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-rugg",
                                    "from_vpos": "center",
                                    "to_stop": "place-rcmnl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-rcmnl",
                                "connections": [
                                  {
                                    "from_stop": "place-rugg",
                                    "from_vpos": "top",
                                    "to_stop": "place-rcmnl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-rcmnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-jaksn",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-jaksn",
                                "connections": [
                                  {
                                    "from_stop": "place-rcmnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-jaksn",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-jaksn",
                                    "from_vpos": "center",
                                    "to_stop": "place-sbmnl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-sbmnl",
                                "connections": [
                                  {
                                    "from_stop": "place-jaksn",
                                    "from_vpos": "top",
                                    "to_stop": "place-sbmnl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-sbmnl",
                                    "from_vpos": "center",
                                    "to_stop": "place-grnst",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-grnst",
                                "connections": [
                                  {
                                    "from_stop": "place-sbmnl",
                                    "from_vpos": "top",
                                    "to_stop": "place-grnst",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  },
                                  {
                                    "from_stop": "place-grnst",
                                    "from_vpos": "center",
                                    "to_stop": "place-forhl",
                                    "to_vpos": "bottom",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              },
                              {
                                "stop_id": "place-forhl",
                                "connections": [
                                  {
                                    "from_stop": "place-grnst",
                                    "from_vpos": "top",
                                    "to_stop": "place-forhl",
                                    "to_vpos": "center",
                                    "from_lane": "center",
                                    "to_lane": "center"
                                  }
                                ],
                                "stop_lane": "center"
                              }
                            ],
                            "typical?": true
                          }
                        ]
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
            val response = RouteStopsRepository().getRouteSegments("Orange", 0)
            assertEquals("/api/route/stop-graph", requestUrl.encodedPath)
            assertEquals("route_id=Orange&direction_id=0", requestUrl.encodedQuery)
            assertEquals(
                ApiResult.Ok(
                    RouteStopsResult(
                        "Orange",
                        0,
                        listOf(
                            RouteBranchSegment(
                                listOf(
                                    RouteBranchSegment.BranchStop(
                                        "place-ogmnl",
                                        RouteBranchSegment.Lane.Center,
                                        listOf(
                                            RouteBranchSegment.StickConnection(
                                                fromStop = "place-ogmnl",
                                                fromLane = RouteBranchSegment.Lane.Center,
                                                fromVPos = RouteBranchSegment.VPos.Center,
                                                toStop = "place-mlmnl",
                                                toLane = RouteBranchSegment.Lane.Center,
                                                toVPos = RouteBranchSegment.VPos.Bottom,
                                            )
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-mlmnl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-ogmnl",
                                            "place-mlmnl",
                                            "place-welln",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-welln",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-mlmnl",
                                            "place-welln",
                                            "place-astao",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-astao",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-welln",
                                            "place-astao",
                                            "place-sull",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-sull",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-astao",
                                            "place-sull",
                                            "place-ccmnl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-ccmnl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-sull",
                                            "place-ccmnl",
                                            "place-north",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-north",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-ccmnl",
                                            "place-north",
                                            "place-haecl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-haecl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-north",
                                            "place-haecl",
                                            "place-state",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-state",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-haecl",
                                            "place-state",
                                            "place-dwnxg",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-dwnxg",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-state",
                                            "place-dwnxg",
                                            "place-chncl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-chncl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-dwnxg",
                                            "place-chncl",
                                            "place-tumnl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-tumnl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-chncl",
                                            "place-tumnl",
                                            "place-bbsta",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-bbsta",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-tumnl",
                                            "place-bbsta",
                                            "place-masta",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-masta",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-bbsta",
                                            "place-masta",
                                            "place-rugg",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-rugg",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-masta",
                                            "place-rugg",
                                            "place-rcmnl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-rcmnl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-rugg",
                                            "place-rcmnl",
                                            "place-jaksn",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-jaksn",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-rcmnl",
                                            "place-jaksn",
                                            "place-sbmnl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-sbmnl",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-jaksn",
                                            "place-sbmnl",
                                            "place-grnst",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-grnst",
                                        RouteBranchSegment.Lane.Center,
                                        RouteBranchSegment.StickConnection.forward(
                                            "place-sbmnl",
                                            "place-grnst",
                                            "place-forhl",
                                            RouteBranchSegment.Lane.Center,
                                        ),
                                    ),
                                    RouteBranchSegment.BranchStop(
                                        "place-forhl",
                                        RouteBranchSegment.Lane.Center,
                                        listOf(
                                            RouteBranchSegment.StickConnection(
                                                fromStop = "place-grnst",
                                                fromLane = RouteBranchSegment.Lane.Center,
                                                fromVPos = RouteBranchSegment.VPos.Top,
                                                toStop = "place-forhl",
                                                toLane = RouteBranchSegment.Lane.Center,
                                                toVPos = RouteBranchSegment.VPos.Center,
                                            )
                                        ),
                                    ),
                                ),
                                name = null,
                                isTypical = true,
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
