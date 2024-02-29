package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.RouteResponse
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
                          "stops": {
                            "8552": {
                              "id": "8552",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289904,
                              "longitude": -71.191003,
                              "parent_station": null
                            },
                            "84791": {
                              "id": "84791",
                              "name": "Sawmill Brook Pkwy @ Walsh Rd",
                              "latitude": 42.289995,
                              "longitude": -71.191092,
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
                                "long_name": "Dedham Mall - Watertown Yard",
                                "short_name": "52",
                                "sort_order": 50520,
                                "text_color": "000000"
                            }
                          },
                          "trips": {
                            "trip1": {
                              "id": "trip1",
                              "headsign": "Watertown",
                              "route_pattern_id": "52-4-0"
                            },
                            "trip2": {
                              "id": "trip2",
                              "headsign": "Charles River Loop",
                              "route_pattern_id": "52-4-1"
                            },
                            "trip3": {
                              "id": "trip3",
                              "headsign": "Watertown",
                              "route_pattern_id": "52-5-0"
                            },
                            "trip4": {
                              "id": "trip4",
                              "headsign": "Dedham Mall",
                              "route_pattern_id": "52-5-1"
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
                TestData.route(
                    id = "52",
                    type = RouteType.BUS,
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
                        mapOf(
                            "8552" to
                                Stop(
                                    id = "8552",
                                    latitude = 42.289904,
                                    longitude = -71.191003,
                                    name = "Sawmill Brook Pkwy @ Walsh Rd"
                                ),
                            "84791" to
                                Stop(
                                    id = "84791",
                                    latitude = 42.289995,
                                    longitude = -71.191092,
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
                                    headsign = "Watertown",
                                    routePatternId = "52-4-0"
                                ),
                            "trip2" to
                                Trip(
                                    id = "trip2",
                                    headsign = "Charles River Loop",
                                    routePatternId = "52-4-1"
                                ),
                            "trip3" to
                                Trip(
                                    id = "trip3",
                                    headsign = "Watertown",
                                    routePatternId = "52-5-0"
                                ),
                            "trip4" to
                                Trip(
                                    id = "trip4",
                                    headsign = "Dedham Mall",
                                    routePatternId = "52-5-1"
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
    fun testGetRailRouteShapes() {
        runBlocking {
            val mockEngine = MockEngine { request ->
                assertEquals("", request.url.encodedQuery)
                respond(
                    content =
                        ByteReadChannel(
                            """
                    {
                      "routes": [
                        {
                          "id": "Red",
                          "type": "heavy_rail",
                          "color": "DA291C",
                          "direction_names": [
                            "South",
                            "North"
                          ],
                          "direction_destinations": [
                            "Ashmont/Braintree",
                            "Alewife"
                          ],
                          "long_name": "Red Line",
                          "short_name": "",
                          "sort_order": 10010,
                          "text_color": "FFFFFF",
                          "route_patterns": [
                            {
                              "id": "Red-1-0",
                              "name": "Alewife - Ashmont",
                              "route": {
                                "id": "Red",
                                "type": "route"
                              },
                              "direction_id": 0,
                              "sort_order": 100100001,
                              "typicality": "typical",
                              "representative_trip": {
                                "id": "canonical-Red-C2-0",
                                "headsign": "Ashmont",
                                "route_pattern": {
                                  "id": "Red-1-0",
                                  "type": "route_pattern"
                                },
                                "shape": {
                                  "id": "canonical-931_0009",
                                  "polyline": "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
                                },
                                "stops": null
                              }
                            },
                            {
                              "id": "Red-1-1",
                              "name": "Ashmont - Alewife",
                              "route": {
                                "id": "Red",
                                "type": "route"
                              },
                              "direction_id": 1,
                              "sort_order": 100101001,
                              "typicality": "typical",
                              "representative_trip": {
                                "id": "canonical-Red-C2-1",
                                "headsign": "Alewife",
                                "route_pattern": {
                                  "id": "Red-1-1",
                                  "type": "route_pattern"
                                },
                                "shape": {
                                  "id": "canonical-931_0010",
                                  "polyline": "qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN"
                                },
                                "stops": null
                              }
                            },
                            {
                              "id": "Red-3-1",
                              "name": "Braintree - Alewife",
                              "route": {
                                "id": "Red",
                                "type": "route"
                              },
                              "direction_id": 1,
                              "sort_order": 100101000,
                              "typicality": "typical",
                              "representative_trip": {
                                "id": "canonical-Red-C1-1",
                                "headsign": "Alewife",
                                "route_pattern": {
                                  "id": "Red-3-1",
                                  "type": "route_pattern"
                                },
                                "shape": {
                                  "id": "canonical-933_0010",
                                  "polyline": "iyr`GzmjpLWAgAGoBQoCMo@EwBSaCKgMeAs@EcDUsEa@wAOqAIg@EeCSqA?[Bs@Ds@H{@JcB\\q@Nw@TiA^mAh@mEvCIFA@MHaGlDqAv@{BlAeHbEg@XULOJWLSJiAr@KFKF[PoBfA{Ar@qD`BmBr@iAXaAVy@P}@Lq@HeAH{@?cAEq@Gk@CSCc@G??qAO}BYMAuAQw@KiBOwBYoCe@mPoCkHmA}@OWCWE_@GkIwA{AWQCMAUEyBa@cGy@oC[_BSuAMiAE{@Bm@FcAPODMDE@YJQDYHq@XeAh@q@\\yBdAi@T??u@\\m@ZmBlAgCjA{@h@w@l@MJKH[Zw@|@wLzMSTQR[XoE~E]`@sDtDoEzEe@d@sAvAsD~DmPxQsF~F}AtA]ZOLOLm@`@IF??yAxAWXkCzCeAtA{HfJiIzIcLzLuC`D}F`G??{@z@uBxB{@`AkCfCgA`Ac@Xw@`@i@Vy@XiAZwDdAqA`@o@PiAj@gAr@aAt@wAnAUV}@fAKJKNEDMNmAvA}BjC_EbFyDnEgDzDgAfAeBnB_F|FIHIJw@~@aAfAcAhAcDrDSTSTg@n@a@b@iL`NmEhFiCxCqLjNcDpD}DvE}HtIgAz@oAz@IDgAh@_A`@_ARy@LkBHy@@wAIuAOqBa@YEsWgE{D}@yCw@}IoCeDcAgF_BkB_@_AK}BEyHXeE@{@@sEJkCF]Bg@@}AFM@a@D_AN??]FSDUBm@FqCd@}@VgAd@eAr@m@f@g@j@{@hAoAhBkBdD[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN"
                                },
                                "stops": null
                              }
                            },
                            {
                              "id": "Red-Z-1",
                              "name": "Ashmont - Broadway",
                              "route": {
                                "id": "Red",
                                "type": "route"
                              },
                              "direction_id": 1,
                              "sort_order": 100101120,
                              "typicality": "atypical",
                              "representative_trip": {
                                "id": "61202958",
                                "headsign": "Broadway",
                                "route_pattern": {
                                  "id": "Red-Z-1",
                                  "type": "route_pattern"
                                },
                                "shape": {
                                  "id": "20320002",
                                  "polyline": "qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@"
                                },
                                "stops": null
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
            val backend = Backend(mockEngine)
            val response = backend.getRailRouteShapes()

            assertEquals(
                RouteResponse(
                    routes =
                        listOf(
                            TestData.route(
                                id = "Red",
                                type = RouteType.HEAVY_RAIL,
                                color = "DA291C",
                                directionNames = listOf("South", "North"),
                                directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
                                longName = "Red Line",
                                shortName = "",
                                sortOrder = 10010,
                                textColor = "FFFFFF",
                                routePatterns =
                                    listOf(
                                        TestData.routePattern(
                                            id = "Red-1-0",
                                            directionId = 0,
                                            name = "Alewife - Ashmont",
                                            sortOrder = 100100001,
                                            typicality = RoutePattern.Typicality.Typical,
                                            representativeTrip =
                                                TestData.trip(
                                                    id = "canonical-Red-C2-0",
                                                    headsign = "Ashmont",
                                                    routePatternId = "Red-1-0",
                                                    shape =
                                                        TestData.shape(
                                                            id = "canonical-931_0009",
                                                            polyline =
                                                                "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
                                                        )
                                                ),
                                            routeId = "Red"
                                        ),
                                        TestData.routePattern(
                                            id = "Red-1-1",
                                            directionId = 1,
                                            name = "Ashmont - Alewife",
                                            sortOrder = 100101001,
                                            typicality = RoutePattern.Typicality.Typical,
                                            representativeTrip =
                                                TestData.trip(
                                                    id = "canonical-Red-C2-1",
                                                    headsign = "Alewife",
                                                    routePatternId = "Red-1-1",
                                                    shape =
                                                        TestData.shape(
                                                            id = "canonical-931_0010",
                                                            polyline =
                                                                "qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN"
                                                        )
                                                ),
                                            routeId = "Red"
                                        ),
                                        TestData.routePattern(
                                            id = "Red-3-1",
                                            name = "Braintree - Alewife",
                                            routeId = "Red",
                                            directionId = 1,
                                            sortOrder = 100101000,
                                            typicality = RoutePattern.Typicality.Typical,
                                            representativeTrip =
                                                TestData.trip(
                                                    id = "canonical-Red-C1-1",
                                                    headsign = "Alewife",
                                                    routePatternId = "Red-3-1",
                                                    shape =
                                                        TestData.shape(
                                                            id = "canonical-933_0010",
                                                            polyline =
                                                                "iyr`GzmjpLWAgAGoBQoCMo@EwBSaCKgMeAs@EcDUsEa@wAOqAIg@EeCSqA?[Bs@Ds@H{@JcB\\q@Nw@TiA^mAh@mEvCIFA@MHaGlDqAv@{BlAeHbEg@XULOJWLSJiAr@KFKF[PoBfA{Ar@qD`BmBr@iAXaAVy@P}@Lq@HeAH{@?cAEq@Gk@CSCc@G??qAO}BYMAuAQw@KiBOwBYoCe@mPoCkHmA}@OWCWE_@GkIwA{AWQCMAUEyBa@cGy@oC[_BSuAMiAE{@Bm@FcAPODMDE@YJQDYHq@XeAh@q@\\yBdAi@T??u@\\m@ZmBlAgCjA{@h@w@l@MJKH[Zw@|@wLzMSTQR[XoE~E]`@sDtDoEzEe@d@sAvAsD~DmPxQsF~F}AtA]ZOLOLm@`@IF??yAxAWXkCzCeAtA{HfJiIzIcLzLuC`D}F`G??{@z@uBxB{@`AkCfCgA`Ac@Xw@`@i@Vy@XiAZwDdAqA`@o@PiAj@gAr@aAt@wAnAUV}@fAKJKNEDMNmAvA}BjC_EbFyDnEgDzDgAfAeBnB_F|FIHIJw@~@aAfAcAhAcDrDSTSTg@n@a@b@iL`NmEhFiCxCqLjNcDpD}DvE}HtIgAz@oAz@IDgAh@_A`@_ARy@LkBHy@@wAIuAOqBa@YEsWgE{D}@yCw@}IoCeDcAgF_BkB_@_AK}BEyHXeE@{@@sEJkCF]Bg@@}AFM@a@D_AN??]FSDUBm@FqCd@}@VgAd@eAr@m@f@g@j@{@hAoAhBkBdD[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@??qCBie@i[y@c@m@QYIu@Gc@?i@DiARm@TQNa@f@uDbNIX??k@hBq@jBs@vBk@bA_JrQUj@??aDfI??[v@Wn@w@pBa@`Aa@fAwBtFc@~@k@hAsCxDwJ~N]|@Mb@K^CFKn@Gb@C\\ATCjAEt@GhBEbA??AXGlAAPM`DYjIM|CO|GM|DG|AGxCa@`MUbHs@fJOfD??ATIrB_Ap^s@jRWlJYdHOhGIpBKjAk@bBe@bAaDnGkBzD??m@pAwMpVgArCsBdGkEbPc@nAYb@a@d@eChDwG|HMPEFwCzNK^KVMJIB_@?iAO??kAOmB]gBGm@A[HSL[^a@|@e@fAo@r@iBh@aB^}@BkAGaNq@am@iDQ???sMUyQ~@uCJsBDo@Pc@Xm@l@m@bAaAvBu@pB]hAOx@UdAKr@??i@dD_@lDKlBOpCQjESvHKtEYbJg@rUGjCRnDFv@NbAPjAHRHLLFt@Nt@Vt@j@f@|@Vt@@DR~B@b@HpAFxN"
                                                        )
                                                )
                                        ),
                                        TestData.routePattern(
                                            id = "Red-Z-1",
                                            name = "Ashmont - Broadway",
                                            routeId = "Red",
                                            directionId = 1,
                                            sortOrder = 100101120,
                                            typicality = RoutePattern.Typicality.Atypical,
                                            representativeTrip =
                                                TestData.trip(
                                                    id = "61202958",
                                                    headsign = "Broadway",
                                                    routePatternId = "Red-Z-1",
                                                    shape =
                                                        TestData.shape(
                                                            id = "20320002",
                                                            polyline =
                                                                "qsaaGfrvpLYLuDxAI?e@MWASBcElAq@LsCZc@HaOfBoOlByB^??yB`@uE|@qBFqA@{AOu@Qo@]iAi@o@a@o@i@k@i@q@u@a@k@]m@c@{@Sc@Uq@_@qAm@wDCSo@sD??G_@Mm@Ge@IeACq@Ac@Ay@AkDAeDEmAMqAI_@Ia@I[Oc@i@iAU]_@_@SO}@g@IAIC{@CaB?uABcAEy@GiAMuAWYGc@KqDu@yHiAgBYO?_AM}@KyA[}@Ui@QmA]kA_@cA_@aA[}Am@??SI]KoBq@_AYoDcAeBc@mASwAOoBEs@?kCL}BDqC@cCFeA@_A@u@Dw@DmADiAF[@sBPyAF??u@DSByAPeAT}@V]Nk@Tm@X_@RcAv@WNo@r@]b@{DtF[h@}A|Bq@hAUZ}B|Cm@d@m@\\{@XY@MC_@QqBq@{F_C??g@ScnAl@"
                                                        )
                                                )
                                        )
                                    )
                            )
                        )
                ),
                response
            )
        }
    }
}
