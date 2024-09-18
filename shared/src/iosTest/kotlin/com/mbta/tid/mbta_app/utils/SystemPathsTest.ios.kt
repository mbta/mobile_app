package com.mbta.tid.mbta_app.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class SystemPathsTest {
    @Test
    fun testPaths() {
        val paths = IOSSystemPaths()
        assertEquals("/Library/Application Support", paths.data.toString())
        assertEquals("/Library/Caches", paths.cache.toString())
    }
}
