package com.mbta.tid.mbta_app.cache

interface CacheFile {
    val cacheKey: String

    fun write(data: String): Boolean

    fun read(): String?

    fun delete(): Boolean
}
