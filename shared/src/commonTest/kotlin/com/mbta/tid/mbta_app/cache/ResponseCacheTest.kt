package com.mbta.tid.mbta_app.cache

import app.cash.turbine.test
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.fs.JsonPersistence
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.mocks.mockJsonPersistence
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.MockSystemPaths
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.path
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ResponseCacheTest {

    @AfterTest fun `stop koin`() = run { stopKoin() }

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
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        var didFetch = false

        startKoin { modules(module { single { mockJsonPersistence() } }) }

        assertEquals(
            ApiResult.Ok(globalData),
            cache.getOrFetch {
                didFetch = true
                client.get { url { path("api/global") } }
            },
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

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")
        cache.data =
            Response(
                ResponseMetadata(
                    null,
                    TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes),
                ),
                globalData,
            )

        startKoin { modules(module { single { mockJsonPersistence() } }) }
        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fail() })
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
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")
        cache.data =
            Response(
                ResponseMetadata(
                    null,
                    TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes),
                ),
                oldData,
            )

        var didFetch = false

        startKoin { modules(module { single { mockJsonPersistence() } }) }

        assertEquals(
            ApiResult.Ok(newData),
            cache.getOrFetch {
                didFetch = true
                client.get { url { path("api/global") } }
            },
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
                            headers = headersOf(HttpHeaders.ETag, matchingEtag),
                        )
                    else ->
                        respond(
                            content = ByteReadChannel(json.encodeToString(newData)),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    Pair(HttpHeaders.ContentType, listOf("application/json")),
                                    Pair(HttpHeaders.ETag, listOf("different etag")),
                                ),
                        )
                }
            } else {
                respond(
                    content = ByteReadChannel(json.encodeToString(oldData)),
                    status = HttpStatusCode.OK,
                    headers =
                        headersOf(
                            Pair(HttpHeaders.ContentType, listOf("application/json")),
                            Pair(HttpHeaders.ETag, listOf(matchingEtag)),
                        ),
                )
            }
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)
        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test", maxAge = 1.seconds)

        var fetchCount = 0
        suspend fun fetch(etag: String?): HttpResponse {
            fetchCount += 1
            return client.get {
                url { path("api/global") }
                header(HttpHeaders.IfNoneMatch, etag)
            }
        }

        startKoin { modules(module { single { mockJsonPersistence() } }) }

        assertEquals(ApiResult.Ok(oldData), cache.getOrFetch(::fetch))
        assertEquals(1, fetchCount)

        // Assert cached
        assertEquals(ApiResult.Ok(oldData), cache.getOrFetch(::fetch))
        assertEquals(1, fetchCount)
        delay(1.seconds)

        // Assert that the cache retains the data when it receives a NotModified response
        assertEquals(ApiResult.Ok(oldData), cache.getOrFetch(::fetch))
        assertEquals(2, fetchCount)

        // And that receiving NotModified resets the cache timer
        assertEquals(ApiResult.Ok(oldData), cache.getOrFetch(::fetch))
        assertEquals(2, fetchCount)
        delay(1.seconds)

        // Get new data on next request
        assertEquals(ApiResult.Ok(newData), cache.getOrFetch(::fetch))
        assertEquals(3, fetchCount)
        delay(1.seconds)

        // Ensure we go back to the old data when sent etag doesn't match
        assertEquals(ApiResult.Ok(oldData), cache.getOrFetch(::fetch))
        assertEquals(4, fetchCount)
    }

    @Test
    fun `returns data from disk`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY
        fileSystem.createDirectories(directory)
        fileSystem.write(directory / "test-meta.json") {
            writeUtf8(
                json.encodeToString(
                    ResponseMetadata(
                        null,
                        TimeSource.Monotonic.markNow().minus(cache.maxAge - 1.minutes),
                    )
                )
            )
        }
        fileSystem.write(directory / "test.json") { writeUtf8(json.encodeToString(globalData)) }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fail() })
        fileSystem.delete(directory / "test.json")
        assertEquals(
            ApiResult.Ok(globalData),
            cache.getOrFetch { fail() },
            "Only the initial get is from disk, after that it's stored in memory",
        )
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
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY)
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test-meta.json") {
            writeUtf8(
                json.encodeToString(
                    ResponseMetadata(
                        null,
                        TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes),
                    )
                )
            )
        }
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
            writeUtf8(json.encodeToString(oldData))
        }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        var didFetch = false

        assertEquals(
            ApiResult.Ok(newData),
            cache.getOrFetch {
                didFetch = true
                client.get { url { path("api/global") } }
            },
        )
        assertTrue(didFetch)
    }

    @Test
    fun `passes stale data into flow immediately`() = runBlocking {
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
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY)
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test-meta.json") {
            writeUtf8(
                json.encodeToString(
                    ResponseMetadata(
                        null,
                        TimeSource.Monotonic.markNow().minus(cache.maxAge + 1.minutes),
                    )
                )
            )
        }
        fileSystem.write(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
            writeUtf8(json.encodeToString(oldData))
        }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        var didFetch = false

        cache.state.test {
            assertNull(awaitItem())
            val result =
                cache.getOrFetch {
                    didFetch = true
                    client.get { url { path("api/global") } }
                }

            assertEquals(oldData, awaitItem())
            assertTrue(didFetch)
            assertEquals(ApiResult.Ok(newData), result)
            assertEquals(newData, awaitItem())
        }
    }

    @Test
    fun `writes to disk cache on load`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val responseEtag = "etag"
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        Pair(HttpHeaders.ContentType, listOf("application/json")),
                        Pair(HttpHeaders.ETag, listOf(responseEtag)),
                    ),
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        var didFetch = false

        assertEquals(
            ApiResult.Ok(globalData),
            cache.getOrFetch {
                didFetch = true
                client.get { url { path("api/global") } }
            },
        )
        assertTrue(didFetch)
        assertEquals(
            cache.data!!.metadata.etag,
            json
                .decodeFromString<ResponseMetadata>(
                    fileSystem.read(
                        mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test-meta.json"
                    ) {
                        readUtf8()
                    }
                )
                .etag,
        )
        assertEquals(
            cache.data!!.body,
            json.decodeFromString(
                fileSystem.read(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
                    readUtf8()
                }
            ),
        )
    }

    @Test
    fun `writes full json even when some of it isn't parsed`() = runBlocking {
        @Serializable data class PartialObject(val a: String, val b: Int)
        @Serializable data class CompleteObject(val a: String, val b: Int, val c: Double)
        @Serializable
        data class FutureObject(val a: String, val b: Int, val c: Double, val d: String)

        val partial = PartialObject("a", 1)
        val complete = CompleteObject("a", 1, 2.0)
        val future = FutureObject("a", 1, 2.0, "d")

        val responseEtag = "etag"
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(json.encodeToString(future)),
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        Pair(HttpHeaders.ContentType, listOf("application/json")),
                        Pair(HttpHeaders.ETag, listOf(responseEtag)),
                    ),
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val partialCache = ResponseCache.create<PartialObject>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        var didFetch = false

        assertEquals(
            ApiResult.Ok(partial),
            partialCache.getOrFetch {
                didFetch = true
                client.get { url { path("api/global") } }
            },
        )
        assertTrue(didFetch)

        assertEquals(
            future,
            json.decodeFromString(
                fileSystem.read(mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test.json") {
                    readUtf8()
                }
            ),
        )

        val completeCache = ResponseCache.create<CompleteObject>(cacheKey = "test")
        assertEquals(
            ApiResult.Ok(complete),
            completeCache.getOrFetch { fail("Should not fetch again") },
        )
    }

    @Test
    fun `cache is invalidated by changing the invalidation key`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalData = GlobalResponse(objects, emptyMap())

        val responseEtag = "etag"
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(json.encodeToString(globalData)),
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        Pair(HttpHeaders.ContentType, listOf("application/json")),
                        Pair(HttpHeaders.ETag, listOf(responseEtag)),
                    ),
            )
        }

        val client = MobileBackendClient(mockEngine, AppVariant.Staging)

        val cache = ResponseCache.create<GlobalResponse>(cacheKey = "test")

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY
        fileSystem.createDirectories(directory)

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        var fetchCount = 0
        suspend fun fetch(): HttpResponse {
            fetchCount++
            return client.get { url { path("api/global") } }
        }

        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fetch() })
        assertEquals(1, fetchCount)
        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fail() })

        fun metadataFromDisk() =
            json.decodeFromString<ResponseMetadata>(
                fileSystem.read(
                    mockPaths.cache / ResponseCache.CACHE_SUBDIRECTORY / "test-meta.json"
                ) {
                    readUtf8()
                }
            )

        assertEquals(responseEtag, metadataFromDisk().etag)
        assertNull(cache.data!!.metadata.invalidationKey)

        val firstInvalidationKey = "key1"
        val invalidatedCache =
            ResponseCache.create<GlobalResponse>(
                cacheKey = "test",
                invalidationKey = firstInvalidationKey,
            )
        assertEquals(ApiResult.Ok(globalData), invalidatedCache.getOrFetch { fetch() })
        assertEquals(2, fetchCount)
        assertEquals(responseEtag, metadataFromDisk().etag)
        assertEquals(firstInvalidationKey, metadataFromDisk().invalidationKey)
        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fail() })

        val secondInvalidationKey = "key2"
        val otherInvalidatedCache =
            ResponseCache.create<GlobalResponse>(
                cacheKey = "test",
                invalidationKey = secondInvalidationKey,
            )
        assertEquals(ApiResult.Ok(globalData), otherInvalidatedCache.getOrFetch { fetch() })
        assertEquals(3, fetchCount)
        assertEquals(responseEtag, metadataFromDisk().etag)
        assertEquals(secondInvalidationKey, metadataFromDisk().invalidationKey)
        assertEquals(ApiResult.Ok(globalData), cache.getOrFetch { fail() })
    }
}
