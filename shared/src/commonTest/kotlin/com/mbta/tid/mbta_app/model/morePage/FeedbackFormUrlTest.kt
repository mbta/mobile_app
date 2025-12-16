package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.PlatformType
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackFormUrlTest {
    @Test
    fun testParams() {
        assertEquals(
            "https://example.com?language=en&version=1.2.3&platform=iOS",
            feedbackFormUrl("https://example.com", "en", "1.2.3", PlatformType.iOS),
        )
    }

    @Test
    fun testAndroid() {
        assertEquals(
            "https://example.com?language=es&version=0.0.0&platform=Android",
            feedbackFormUrl("https://example.com", "es", "0.0.0", PlatformType.Android),
        )
    }

    @Test
    fun testNullVersion() {
        assertEquals(
            "https://example.com?language=ht&version=null&platform=Android",
            feedbackFormUrl("https://example.com", "ht", null, PlatformType.JVM),
        )
    }
}
