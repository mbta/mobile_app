package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.path
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResponseCacheTest {
    @Test
    fun `fetches data if empty`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(Json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache()

        var didFetch = false

        assertEquals(
            globalData,
            cache
                .getOrFetch {
                    didFetch = true
                    client.get { url { path("api/global") } }
                }
                .body()
        )
        assertTrue(didFetch)
    }

    @Test
    fun `returns data within timeout`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(Json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)
        val httpResponse = client.get { url { path("api/global") } }

        val cache = ResponseCache()
        cache.data = httpResponse
        cache.dataTimestamp = TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes)

        assertEquals(httpResponse, cache.getOrFetch { fail() })
    }

    @Test
    fun `fetches data beyond timeout`() = runBlocking {
        val oldObjects = ObjectCollectionBuilder()
        val oldData = GlobalResponse(oldObjects, emptyMap())

        val newObjects = ObjectCollectionBuilder()
        newObjects.stop()
        newObjects.stop()
        newObjects.route()
        val newData = GlobalResponse(newObjects, emptyMap())

        var callIndex = 0
        val mockEngine = MockEngine { request ->
            callIndex += 1
            when (callIndex) {
                1 ->
                    respond(
                        content = ByteReadChannel(Json.encodeToString(oldData)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                else ->
                    respond(
                        content = ByteReadChannel(Json.encodeToString(newData)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
            }
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache()
        cache.data = client.get { url { path("api/global") } }
        cache.dataTimestamp = TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes)

        var didFetch = false

        val newResponse = client.get { url { path("api/global") } }

        assertEquals(
            newResponse,
            cache.getOrFetch {
                didFetch = true
                newResponse
            }
        )
        assertTrue(didFetch)
    }

    @Test
    fun `returns existing data when etag matches`() = runBlocking {
        val oldObjects = ObjectCollectionBuilder()
        val oldData = GlobalResponse(oldObjects, emptyMap())

        val newObjects = ObjectCollectionBuilder()
        newObjects.stop()
        newObjects.stop()
        newObjects.route()
        val newData = GlobalResponse(newObjects, emptyMap())

        val matchingEtag = "matching etag"

        var callIndex = 0
        val mockEngine = MockEngine { request ->
            if (request.headers[HttpHeaders.IfNoneMatch] == matchingEtag) {
                callIndex += 1
                when (callIndex) {
                    1 ->
                        respond(
                            content = "",
                            status = HttpStatusCode.NotModified,
                            headers = headersOf(HttpHeaders.ETag, matchingEtag)
                        )
                    else ->
                        respond(
                            content = ByteReadChannel(Json.encodeToString(newData)),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    Pair(HttpHeaders.ContentType, listOf("application/json")),
                                    Pair(HttpHeaders.ETag, listOf("different etag"))
                                )
                        )
                }
            } else {
                respond(
                    content = ByteReadChannel(Json.encodeToString(oldData)),
                    status = HttpStatusCode.OK,
                    headers =
                        headersOf(
                            Pair(HttpHeaders.ContentType, listOf("application/json")),
                            Pair(HttpHeaders.ETag, listOf(matchingEtag))
                        )
                )
            }
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)
        val cache = ResponseCache(maxAge = 1.seconds)

        var fetchCount = 0
        suspend fun fetch(etag: String?): HttpResponse {
            fetchCount += 1
            return client.get {
                url { path("api/global") }
                header(HttpHeaders.IfNoneMatch, etag)
            }
        }

        assertEquals(oldData, cache.getOrFetch(::fetch).body())
        assertEquals(1, fetchCount)

        // Assert cached
        assertEquals(oldData, cache.getOrFetch(::fetch).body())
        assertEquals(1, fetchCount)
        delay(1.seconds)

        // Assert that the cache retains the data when it receives a NotModified response
        assertEquals(oldData, cache.getOrFetch(::fetch).body())
        assertEquals(2, fetchCount)

        // And that receiving NotModified resets the cache timer
        assertEquals(oldData, cache.getOrFetch(::fetch).body())
        assertEquals(2, fetchCount)
        delay(1.seconds)

        // Get new data on next request
        assertEquals(newData, cache.getOrFetch(::fetch).body())
        assertEquals(3, fetchCount)
        delay(1.seconds)

        // Ensure we go back to the old data when sent etag doesn't match
        assertEquals(oldData, cache.getOrFetch(::fetch).body())
        assertEquals(4, fetchCount)
    }
}
