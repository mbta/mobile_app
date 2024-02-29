package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.util.formatShortTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant

class IosTimeFormatterTest {

    @Test
    fun testExample() {
        val time = Instant.fromEpochMilliseconds(1709221722000)
        assertEquals("10:48 AM", formatShortTime(time))
    }
}
