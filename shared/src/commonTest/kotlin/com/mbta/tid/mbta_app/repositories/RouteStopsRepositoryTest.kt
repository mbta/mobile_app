package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
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
    fun testGetSchedule() {
        lateinit var requestUrl: Url
        val mockEngine = MockEngine { request ->
            requestUrl = request.url
            respond(
                content =
                    ByteReadChannel(
                        """
                    {
                        "stop_ids": [
                            "place-ogmnl",
                            "place-mlmnl",
                            "place-welln",
                            "place-astao",
                            "place-sull",
                            "place-ccmnl",
                            "place-north",
                            "place-haecl",
                            "place-state",
                            "place-dwnxg",
                            "place-chncl",
                            "place-tumnl",
                            "place-bbsta",
                            "place-masta",
                            "place-rugg",
                            "place-rcmnl",
                            "place-jaksn",
                            "place-sbmnl",
                            "place-grnst",
                            "place-forhl"
                        ]
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
            val response = RouteStopsRepository().getRouteStops("Orange", 0)
            assertEquals("/api/route/stops", requestUrl.encodedPath)
            assertEquals("route_id=Orange&direction_id=0", requestUrl.encodedQuery)
            assertEquals(
                ApiResult.Ok(
                    RouteStopsResult(
                        "Orange",
                        0,
                        listOf(
                            "place-ogmnl",
                            "place-mlmnl",
                            "place-welln",
                            "place-astao",
                            "place-sull",
                            "place-ccmnl",
                            "place-north",
                            "place-haecl",
                            "place-state",
                            "place-dwnxg",
                            "place-chncl",
                            "place-tumnl",
                            "place-bbsta",
                            "place-masta",
                            "place-rugg",
                            "place-rcmnl",
                            "place-jaksn",
                            "place-sbmnl",
                            "place-grnst",
                            "place-forhl",
                        ),
                    )
                ),
                response,
            )
        }
        stopKoin()
    }
}
