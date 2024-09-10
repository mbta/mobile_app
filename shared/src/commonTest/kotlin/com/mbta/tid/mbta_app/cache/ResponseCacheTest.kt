package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
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
                content = ByteReadChannel(json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache(cacheKey = "test")

        var didFetch = false

        assertEquals(
            globalData,
            json.decodeFromString(
                cache.getOrFetch {
                    didFetch = true
                    client.get { url { path("api/global") } }
                }
            )
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
                content = ByteReadChannel(json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)
        val httpResponse = client.get { url { path("api/global") } }

        val cache = ResponseCache(cacheKey = "test")
        cache.data =
            Response(
                null,
                TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes),
                json.encodeToString(globalData)
            )

        assertEquals(globalData, json.decodeFromString(cache.getOrFetch { fail() }))
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

        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(json.encodeToString(newData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache(cacheKey = "test")
        cache.data =
            Response(
                null,
                TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes),
                json.encodeToString(oldData)
            )

        var didFetch = false

        assertEquals(
            newData,
            json.decodeFromString(
                cache.getOrFetch {
                    didFetch = true
                    client.get { url { path("api/global") } }
                }
            )
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
                            content = ByteReadChannel(json.encodeToString(newData)),
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
                    content = ByteReadChannel(json.encodeToString(oldData)),
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
        val cache = ResponseCache(cacheKey = "test", maxAge = 1.seconds)

        var fetchCount = 0
        suspend fun fetch(etag: String?): HttpResponse {
            fetchCount += 1
            return client.get {
                url { path("api/global") }
                header(HttpHeaders.IfNoneMatch, etag)
            }
        }

        assertEquals(oldData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(1, fetchCount)

        // Assert cached
        assertEquals(oldData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(1, fetchCount)
        delay(1.seconds)

        // Assert that the cache retains the data when it receives a NotModified response
        assertEquals(oldData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(2, fetchCount)

        // And that receiving NotModified resets the cache timer
        assertEquals(oldData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(2, fetchCount)
        delay(1.seconds)

        // Get new data on next request
        assertEquals(newData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(3, fetchCount)
        delay(1.seconds)

        // Ensure we go back to the old data when sent etag doesn't match
        assertEquals(oldData, json.decodeFromString(cache.getOrFetch(::fetch)))
        assertEquals(4, fetchCount)
    }
}
