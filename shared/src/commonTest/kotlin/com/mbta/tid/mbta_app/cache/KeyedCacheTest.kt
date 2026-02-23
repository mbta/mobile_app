package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.fs.JsonPersistence
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.mocks.mockJsonPersistence
import com.mbta.tid.mbta_app.utils.MockSystemPaths
import io.ktor.client.engine.mock.MockEngine.Companion.invoke
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class KeyedCacheTest {
    companion object {
        private const val CACHE_GROUP = "group"
        private const val CACHE_PREFIX = "prefix"
    }

    @AfterTest fun `stop koin`() = run { stopKoin() }

    @Serializable
    private class TestCacheEntry(val data: String, val key: String, val stale: Boolean)

    @Test
    fun `returns null if key doesn't have cache data`() = runBlocking {
        val cache =
            KeyedCache<TestCacheEntry>(
                CACHE_GROUP,
                CACHE_PREFIX,
                json.serializersModule.serializer(),
            )

        startKoin { modules(module { single { mockJsonPersistence() } }) }

        assertNull(cache.getEntry("key") { false })
    }

    @Test
    fun `returns cached data with matching key`() = runBlocking {
        val cache =
            KeyedCache<TestCacheEntry>(
                CACHE_GROUP,
                CACHE_PREFIX,
                json.serializersModule.serializer(),
            )

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / CACHE_GROUP

        val cachedEntry = TestCacheEntry("data", "id", false)

        fileSystem.createDirectories(directory)
        fileSystem.write(directory / "$CACHE_PREFIX-${cachedEntry.key}.json") {
            writeUtf8(json.encodeToString(cachedEntry))
        }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        assertEquals(cachedEntry.data, cache.getEntry(cachedEntry.key) { it.stale }?.data)
    }

    @Test
    fun `writes to disk cache on put`() = runBlocking {
        val cache =
            KeyedCache<TestCacheEntry>(
                CACHE_GROUP,
                CACHE_PREFIX,
                json.serializersModule.serializer(),
            )

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / CACHE_GROUP

        val cachedEntry = TestCacheEntry("data", "id", false)

        fileSystem.createDirectories(directory)

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        cache.putEntry(cachedEntry.key, cachedEntry)

        assertEquals(
            cachedEntry.data,
            json
                .decodeFromString(
                    json.serializersModule.serializer<TestCacheEntry>(),
                    fileSystem.read(
                        mockPaths.cache / CACHE_GROUP / "$CACHE_PREFIX-${cachedEntry.key}.json"
                    ) {
                        readUtf8()
                    },
                )
                .data,
        )
    }

    @Test
    fun `deletes from cache when stale on get`() = runBlocking {
        val cache =
            KeyedCache<TestCacheEntry>(
                CACHE_GROUP,
                CACHE_PREFIX,
                json.serializersModule.serializer(),
            )

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / CACHE_GROUP

        val cachedEntry = TestCacheEntry("data", "id", true)

        val filePath = directory / "$CACHE_PREFIX-${cachedEntry.key}.json"
        fileSystem.createDirectories(directory)
        fileSystem.write(filePath) { writeUtf8(json.encodeToString(cachedEntry)) }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        assertNull(cache.getEntry(cachedEntry.key) { it.stale })
        assertFalse(fileSystem.exists(filePath))
    }

    @Test
    fun `deletes from cache on invalidation`() = runBlocking {
        val cache =
            KeyedCache<TestCacheEntry>(
                CACHE_GROUP,
                CACHE_PREFIX,
                json.serializersModule.serializer(),
            )

        val mockPaths = MockSystemPaths()
        val fileSystem = FakeFileSystem()
        val directory = mockPaths.cache / CACHE_GROUP

        val cachedEntry1 = TestCacheEntry("data1", "id1", true)
        val cachedEntry2 = TestCacheEntry("data2", "id2", false)

        val filePath1 = directory / "$CACHE_PREFIX-${cachedEntry1.key}.json"
        val filePath2 = directory / "$CACHE_PREFIX-${cachedEntry2.key}.json"
        fileSystem.createDirectories(directory)
        fileSystem.write(filePath1) { writeUtf8(json.encodeToString(cachedEntry1)) }
        fileSystem.write(filePath2) { writeUtf8(json.encodeToString(cachedEntry2)) }

        startKoin {
            modules(module { single { JsonPersistence(fileSystem, mockPaths, Dispatchers.IO) } })
        }

        cache.deleteStaleEntries { it.stale }
        assertFalse(fileSystem.exists(filePath1))
        assertTrue(fileSystem.exists(filePath2))
        assertEquals(cachedEntry2.data, cache.getEntry(cachedEntry2.key) { it.stale }?.data)
    }
}
