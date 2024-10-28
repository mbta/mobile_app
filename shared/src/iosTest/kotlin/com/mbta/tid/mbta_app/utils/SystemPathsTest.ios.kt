package com.mbta.tid.mbta_app.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPathsTest {
    @Test
    fun testPaths() {
        val paths = IOSSystemPaths()
        assertTrue(paths.data.toString().endsWith("/Library/Application Support"))
        assertTrue(paths.cache.toString().endsWith("/Library/Caches"))
    }
}
