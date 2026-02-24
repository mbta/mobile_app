package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.fs.JsonPersistence
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.utils.SystemPaths
import kotlin.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface IKeyedCache<T> {
    public suspend fun getEntry(key: String, isStale: (T) -> Boolean): T?

    public suspend fun putEntry(key: String, entry: T)

    public suspend fun deleteStaleEntries(isStale: (T) -> Boolean)
}

/**
 * Implementation of [IKeyedCache] that persists data using JSON files.
 *
 * @param cacheGroup The directory name within the cache file store. This must be unique across
 *   every instance, or [deleteStaleEntries] could delete unexpected data if the types are the same,
 *   or cause deletion to always fail if the types are different.
 * @param keyPrefix A prefix added to each cache file name, joined with the key passed into
 *   [getEntry] and [putEntry].
 * @param serializer The [KSerializer] used to serialize and deserialize [T].
 */
internal class KeyedCache<T : Any>(
    private val cacheGroup: String,
    private val keyPrefix: String,
    private val serializer: KSerializer<T>,
) : IKeyedCache<T>, KoinComponent {
    companion object {
        /**
         * Creates a [KeyedCache] instance for the given type [T].
         *
         * @param cacheGroup The directory name within the cache file store. This must be unique
         *   across every instance, or [deleteStaleEntries] could delete unexpected data if the
         *   types are the same, or cause deletion to always fail if the types are different.
         * @param keyPrefix A prefix added to each cache file name, joined with the key passed into
         *   [getEntry] and [putEntry].
         */
        inline fun <reified T : Any> create(cacheGroup: String, keyPrefix: String) =
            KeyedCache(cacheGroup, keyPrefix, json.serializersModule.serializer())
    }

    private val jsonPersistence: JsonPersistence by inject()

    private fun destination(key: String) =
        JsonPersistence.Destination(SystemPaths.Category.Cache, cacheGroup, "$keyPrefix-$key")

    override suspend fun getEntry(key: String, isStale: (T) -> Boolean): T? {
        val cacheDestination = destination(key)
        try {
            val cached = jsonPersistence.read(cacheDestination, serializer) ?: return null
            if (isStale(cached)) {
                jsonPersistence.delete(cacheDestination)
                return null
            }
            return cached
        } catch (e: Exception) {
            println("Failed to read keyed cache data for $cacheGroup with key $key: $e")
            return null
        }
    }

    override suspend fun putEntry(key: String, entry: T) {
        try {
            jsonPersistence.write(destination(key), serializer, entry)
        } catch (e: Exception) {
            println("Failed to write keyed cache data for $cacheGroup with key $key: $e")
        }
    }

    override suspend fun deleteStaleEntries(isStale: (T) -> Boolean) {
        try {
            jsonPersistence.deleteStale(
                SystemPaths.Category.Cache,
                cacheGroup,
                keyPrefix,
                serializer,
            ) {
                isStale(it)
            }
        } catch (e: Exception) {
            println("Failed to clear stale cache data for $cacheGroup: $e")
        }
    }
}

internal class MockKeyedCache<T : Any>(val cacheMap: MutableMap<String, T> = mutableMapOf()) :
    IKeyedCache<T>, KoinComponent {
    override suspend fun getEntry(key: String, isStale: (T) -> Boolean): T? {
        val entry = cacheMap.getOrElse(key) { null }
        if (entry?.let { isStale(it) } ?: false) {
            cacheMap.remove(key)
            return null
        }
        return entry
    }

    override suspend fun putEntry(key: String, entry: T) {
        cacheMap[key] = entry
    }

    override suspend fun deleteStaleEntries(isStale: (T) -> Boolean) {
        cacheMap.forEach { if (isStale(it.value)) cacheMap.remove(it.key) }
    }
}
