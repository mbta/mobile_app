package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.MockSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
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
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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

        startKoin {
            modules(
                module {
                    single<SystemPaths> { MockSystemPaths(data = "data", cache = "cache") }
                    single<FileSystem> { FakeFileSystem() }
                }
            )
        }
        runBlocking {
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

        stopKoin()
    }

    @Test
    fun `returns data within timeout`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val cache = ResponseCache(cacheKey = "test")
        cache.data =
            Response(
                null,
                TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes),
                json.encodeToString(globalData)
            )

        startKoin {
            modules(
                module {
                    single<SystemPaths> { MockSystemPaths(data = "data", cache = "cache") }
                    single<FileSystem> { FakeFileSystem() }
                }
            )
        }
        runBlocking { assertEquals(globalData, json.decodeFromString(cache.getOrFetch { fail() })) }

        stopKoin()
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

        startKoin {
            modules(
                module {
                    single<SystemPaths> { MockSystemPaths(data = "data", cache = "cache") }
                    single<FileSystem> { FakeFileSystem() }
                }
            )
        }
        runBlocking {
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

        stopKoin()
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

        startKoin {
            modules(
                module {
                    single<SystemPaths> { MockSystemPaths(data = "data", cache = "cache") }
                    single<FileSystem> { FakeFileSystem() }
                }
            )
        }
        runBlocking {
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

        stopKoin()
    }

    @Test
    fun `returns data from disk`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val cache = ResponseCache(cacheKey = "test")

        val encodedResponse =
            json.encodeToString(
                Response(
                    null,
                    TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes),
                    json.encodeToString(globalData)
                )
            )

        val mockPaths = MockSystemPaths(data = "data", cache = "cache")
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY)
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
            writeUtf8(encodedResponse)
        }

        startKoin {
            modules(
                module {
                    single<SystemPaths> { mockPaths }
                    single<FileSystem> { fileSystem }
                }
            )
        }

        runBlocking { assertEquals(globalData, json.decodeFromString(cache.getOrFetch { fail() })) }

        stopKoin()
    }

    @Test
    fun `fetches data when disk cache is stale`() = runBlocking {
        val oldObjects = ObjectCollectionBuilder()
        val oldData = GlobalResponse(oldObjects, emptyMap())

        val newObjects = ObjectCollectionBuilder()
        newObjects.stop()
        newObjects.stop()
        newObjects.route()
        val newData = GlobalResponse(newObjects, emptyMap())

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(json.encodeToString(newData)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache(cacheKey = "test")

        val encodedResponse =
            json.encodeToString(
                Response(
                    null,
                    TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes),
                    json.encodeToString(oldData)
                )
            )

        val mockPaths = MockSystemPaths(data = "data", cache = "cache")
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY)
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
            writeUtf8(encodedResponse)
        }

        startKoin {
            modules(
                module {
                    single<SystemPaths> { mockPaths }
                    single<FileSystem> { fileSystem }
                }
            )
        }

        runBlocking {
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

        stopKoin()
    }

    @Test
    fun `writes to disk cache on load`() = runBlocking {
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

        val mockPaths = MockSystemPaths(data = "data", cache = "cache")
        val fileSystem = FakeFileSystem()

        startKoin {
            modules(
                module {
                    single<SystemPaths> { mockPaths }
                    single<FileSystem> { fileSystem }
                }
            )
        }

        runBlocking {
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
            assertEquals(
                cache.data!!.data,
                json
                    .decodeFromString<Response>(
                        fileSystem.read(
                            mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json"
                        ) {
                            readUtf8()
                        }
                    )
                    .data
            )
        }

        stopKoin()
    }
}
