package com.mbta.tid.mbta_app.utils

import android.content.Context
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemPathsTest {
    @Test
    fun testPaths() {
        val mockContext =
            mock<Context> {
                every { cacheDir } returns File("/", "context_cache")
                every { filesDir } returns File("/", "context_data")
            }
        val paths = AndroidSystemPaths(context = mockContext)
        assertEquals("/context_data", paths.data.toString())
        assertEquals("/context_cache", paths.cache.toString())
    }
}
