package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class GlobalCacheTest {
    @Test
    fun `fetches data if empty`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalResponse = GlobalResponse(objects, emptyMap())

        val cache = GlobalCache()

        var didFetch = false

        assertEquals(
            globalResponse,
            cache.getOrFetch {
                didFetch = true
                globalResponse
            }
        )
        assertTrue(didFetch)
    }

    @Test
    fun `returns data within timeout`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        objects.stop()
        objects.stop()
        objects.route()
        val globalResponse = GlobalResponse(objects, emptyMap())

        val cache = GlobalCache()
        cache.data = globalResponse
        cache.dataTimestamp = Clock.System.now() - (GlobalCache.maxAge - 1.minutes)

        assertEquals(globalResponse, cache.getOrFetch { fail() })
    }

    @Test
    fun `fetches data beyond timeout`() = runBlocking {
        val oldObjects = ObjectCollectionBuilder()
        val oldResponse = GlobalResponse(oldObjects, emptyMap())

        val cache = GlobalCache()
        cache.data = oldResponse
        cache.dataTimestamp = Clock.System.now() - (GlobalCache.maxAge + 1.minutes)

        val newObjects = ObjectCollectionBuilder()
        newObjects.stop()
        newObjects.stop()
        newObjects.route()
        val newResponse = GlobalResponse(newObjects, emptyMap())

        var didFetch = false

        assertEquals(
            newResponse,
            cache.getOrFetch {
                didFetch = true
                newResponse
            }
        )
        assertTrue(didFetch)
    }
}
