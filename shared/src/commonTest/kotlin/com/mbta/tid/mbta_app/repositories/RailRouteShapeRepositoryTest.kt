package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.mocks.mockJsonPersistence
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePatternKey
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class RailRouteShapeRepositoryTest : KoinTest {
    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testGetRailRouteShapes() {
        val mockEngine = MockEngine { _ ->
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
          "direction_id": 0,
          "shape": {
            "id": "canonical-931_0009",
            "polyline": "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM"
          },
          "source_route_pattern_id": "Red-1-0",
          "source_route_id": "Red",
          "route_segments": [
            {
              "id": "place-alfcl-place-asmnl",
              "stop_ids": [
                "place-alfcl",
                "place-davis",
                "place-portr",
                "place-harsq",
                "place-cntsq",
                "place-knncl",
                "place-chmnl",
                "place-pktrm",
                "place-dwnxg",
                "place-sstat",
                "place-brdwy",
                "place-andrw",
                "place-jfk",
                "place-shmnl",
                "place-fldcr",
                "place-smmnl",
                "place-asmnl"
              ],
              "source_route_pattern_id": "Red-1-0",
              "source_route_id": "Red",
              "other_patterns_by_stop_id": {
                "place-alfcl": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-andrw": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-brdwy": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-chmnl": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-cntsq": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-davis": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-dwnxg": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-harsq": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-jfk": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-knncl": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-pktrm": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-portr": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
                ],
                "place-sstat": [
                  { "route_id": "Red", "route_pattern_id": "Red-3-0" }
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
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(
                module {
                    single { MobileBackendClient(mockEngine, AppVariant.Staging) }
                    single { mockJsonPersistence() }
                }
            )
        }
        runBlocking {
            val repo = RailRouteShapeRepository()
            assertNull(repo.state.value)
            val response = repo.getRailRouteShapes()
            val shape =
                Shape(
                    "canonical-931_0009",
                    "}nwaG~|eqLGyNIqAAc@S_CAEWu@g@}@u@k@u@Wu@OMGIMISQkAOcAGw@SoDFkCf@sUXcJJuERwHPkENqCJmB^mDn@}D??D[TeANy@\\iAt@qB`AwBl@cAl@m@b@Yn@QrBEtCKxQ_ApMT??R?`m@hD`Np@jAF|@C`B_@hBi@n@s@d@gA`@}@Z_@RMZIl@@fBFlB\\tAP??~@L^?HCLKJWJ_@vC{NDGLQvG}HdCiD`@e@Xc@b@oAjEcPrBeGfAsCvMqVl@sA??jByD`DoGd@cAj@cBJkAHqBNiGXeHVmJr@kR~@q^HsB@U??NgDr@gJTcH`@aMFyCF}AL}DN}GL}CXkILaD@QFmA@[??DaAFiBDu@BkA@UB]Fc@Jo@BGJ_@Lc@\\}@vJ_OrCyDj@iAb@_AvBuF`@gA`@aAv@qBVo@Xu@??bDgI??Tm@~IsQj@cAr@wBp@kBj@kB??HWtDcN`@g@POl@UhASh@Eb@?t@FXHl@Px@b@he@h[pCC??bnAm@h@T??xF|BpBp@^PLBXAz@Yl@]l@e@|B}CT[p@iA|A}BZi@zDuF\\c@n@s@VObAw@^Sl@Yj@U\\O|@WdAUxAQRCt@E??xAGrBQZAhAGlAEv@Et@E~@AdAAbCGpCA|BEjCMr@?nBDvANlARdBb@nDbA~@XnBp@\\JRH??|Al@`AZbA^jA^lA\\h@P|@TxAZ|@J~@LN?fBXxHhApDt@b@JXFtAVhALx@FbADtAC`B?z@BHBH@|@f@RN^^T\\h@hANb@HZH`@H^LpADlA@dD@jD@x@@b@Bp@HdAFd@Ll@F^??n@rDBRl@vD^pATp@Rb@b@z@\\l@`@j@p@t@j@h@n@h@n@`@hAh@n@\\t@PzANpAApBGtE}@xBa@??xB_@nOmB`OgBb@IrC[p@MbEmARCV@d@LH?tDyAXM",
                )
            val braintreeBranchKey =
                listOf(RoutePatternKey(routeId = Route.Id("Red"), routePatternId = "Red-3-0"))
            val otherPatternsByStopId =
                mapOf(
                    "place-alfcl" to braintreeBranchKey,
                    "place-andrw" to braintreeBranchKey,
                    "place-brdwy" to braintreeBranchKey,
                    "place-chmnl" to braintreeBranchKey,
                    "place-cntsq" to braintreeBranchKey,
                    "place-davis" to braintreeBranchKey,
                    "place-dwnxg" to braintreeBranchKey,
                    "place-harsq" to braintreeBranchKey,
                    "place-jfk" to braintreeBranchKey,
                    "place-knncl" to braintreeBranchKey,
                    "place-pktrm" to braintreeBranchKey,
                    "place-portr" to braintreeBranchKey,
                    "place-sstat" to braintreeBranchKey,
                )
            val stopIds =
                listOf(
                    "place-alfcl",
                    "place-davis",
                    "place-portr",
                    "place-harsq",
                    "place-cntsq",
                    "place-knncl",
                    "place-chmnl",
                    "place-pktrm",
                    "place-dwnxg",
                    "place-sstat",
                    "place-brdwy",
                    "place-andrw",
                    "place-jfk",
                    "place-shmnl",
                    "place-fldcr",
                    "place-smmnl",
                    "place-asmnl",
                )
            val routeSegment =
                RouteSegment(
                    "place-alfcl-place-asmnl",
                    "Red-1-0",
                    Route.Id("Red"),
                    stopIds,
                    otherPatternsByStopId,
                )
            val segmentedRouteShape =
                SegmentedRouteShape("Red-1-0", Route.Id("Red"), 0, listOf(routeSegment), shape)
            val expectedResponse =
                MapFriendlyRouteResponse(
                    listOf(
                        MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                            Route.Id("Red"),
                            listOf(segmentedRouteShape),
                        )
                    )
                )
            assertEquals(ApiResult.Ok(expectedResponse), response)
            assertEquals(expectedResponse, repo.state.value)
        }
    }
}
