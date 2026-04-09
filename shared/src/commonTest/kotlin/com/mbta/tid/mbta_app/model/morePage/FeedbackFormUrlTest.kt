package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.PlatformType
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackFormUrlTest {
    @Test
    fun testParams() {
        assertEquals(
            "https://example.com?language=en&version=1.2.3&platform=iOS&Notifications=true&HideMaps=false",
            feedbackFormUrl(
                "https://example.com",
                "en",
                "1.2.3",
                PlatformType.iOS,
                mapOf(Settings.Notifications to true, Settings.HideMaps to false),
            ),
        )
    }

    @Test
    fun testAndroid() {
        assertEquals(
            "https://example.com?language=es&version=0.0.0&platform=Android",
            feedbackFormUrl("https://example.com", "es", "0.0.0", PlatformType.Android, emptyMap()),
        )
    }

    @Test
    fun testNullVersion() {
        assertEquals(
            "https://example.com?language=ht&version=null&platform=Android",
            feedbackFormUrl("https://example.com", "ht", null, PlatformType.JVM, emptyMap()),
        )
    }
}
