package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
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
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class SearchResultRepositoryTest : KoinTest {
    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testGetSearchResults() {
        val testQuery = "Red"
        val mockEngine = MockEngine { request ->
            assertEquals("query=$testQuery", request.url.encodedQuery)
            respond(
                content =
                    ByteReadChannel(
                        """
{
  "data": {
    "routes": [
      {
        "id": "132",
        "name": "132",
        "type": "route",
        "route_type": "bus",
        "long_name": "Redstone Shopping Center - Malden Center Station",
        "rank": 5
      },
      {
        "id": "Red",
        "name": "Red Line",
        "type": "route",
        "route_type": "heavy_rail",
        "long_name": "Red Line",
        "rank": 2
      }
    ],
    "stops": [
      {
        "id": "25989",
        "name": "Redstone Shopping Center Access Rd",
        "type": "stop",
        "routes": [{ "type": "bus", "icon": "bus" }],
        "rank": 5,
        "zone": null,
        "station?": false
      },
      {
        "id": "15989",
        "name": "Redstone Shopping Center",
        "type": "stop",
        "routes": [{ "type": "bus", "icon": "bus" }],
        "rank": 5,
        "zone": null,
        "station?": false
      }
    ]
  }
}
                        """
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }
        runBlocking {
            val response = SearchResultRepository().getSearchResults(testQuery)
            val expectedResponse =
                SearchResults(
                    listOf(
                        RouteResult(
                            "132",
                            5,
                            "Redstone Shopping Center - Malden Center Station",
                            "132",
                            RouteType.BUS,
                        ),
                        RouteResult("Red", 2, "Red Line", "Red Line", RouteType.HEAVY_RAIL),
                    ),
                    listOf(
                        StopResult(
                            "25989",
                            5,
                            "Redstone Shopping Center Access Rd",
                            null,
                            false,
                            listOf(StopResultRoute(RouteType.BUS, "bus")),
                        ),
                        StopResult(
                            "15989",
                            5,
                            "Redstone Shopping Center",
                            null,
                            false,
                            listOf(StopResultRoute(RouteType.BUS, "bus")),
                        ),
                    ),
                )
            assertEquals(ApiResult.Ok(expectedResponse), response)
        }
    }

    @Test
    fun testRouteSearchParams() {
        val testQuery = "Red"
        val mockEngine = MockEngine { request ->
            assertEquals(
                "query=$testQuery&line_id=Red&type=light_rail%2Cheavy_rail",
                request.url.encodedQuery,
            )
            respond(
                content =
                    ByteReadChannel(
                        """
{
  "data": {
    "routes": [
      {
        "id": "Red",
        "name": "Red Line",
        "type": "route",
        "route_type": "heavy_rail",
        "long_name": "Red Line",
        "rank": 2
      }
    ],
    "stops": []
  }
}
                        """
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }
        runBlocking {
            val response =
                SearchResultRepository()
                    .getRouteFilterResults(
                        testQuery,
                        listOf("Red"),
                        listOf(RouteType.LIGHT_RAIL, RouteType.HEAVY_RAIL),
                    )
            val expectedResponse =
                SearchResults(
                    listOf(RouteResult("Red", 2, "Red Line", "Red Line", RouteType.HEAVY_RAIL)),
                    emptyList(),
                )
            assertEquals(ApiResult.Ok(expectedResponse), response)
        }
    }
}
