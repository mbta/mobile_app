package com.mbta.tid.mbta_app.mocks

import com.mbta.tid.mbta_app.cache.CacheFile

class MockCacheFile(override val cacheKey: String = "test", var data: String? = null) : CacheFile {
    override fun write(data: String): Boolean {
        this.data = data
        return true
    }

    override fun read(): String? {
        return data
    }

    override fun delete(): Boolean {
        this.data = null
        return true
    }
}
