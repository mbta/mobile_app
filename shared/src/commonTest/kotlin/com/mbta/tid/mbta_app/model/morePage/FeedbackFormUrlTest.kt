package com.mbta.tid.mbta_app.model.morePage

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackFormUrlTest {
    @Test
    fun testEN() {
        assertEquals("https://mbta.com/androidappfeedback", feedbackFormUrl("en"))
    }

    @Test
    fun testES() {
        assertEquals("https://mbta.com/androidappfeedback?lang=es-US", feedbackFormUrl("es"))
    }

    @Test
    fun testHT() {
        assertEquals("https://mbta.com/androidappfeedback", feedbackFormUrl("ht"))
    }

    @Test
    fun testPTBR() {
        assertEquals("https://mbta.com/androidappfeedback?lang=pt-BR", feedbackFormUrl("pt-BR"))
    }

    @Test
    fun testVI() {
        assertEquals("https://mbta.com/androidappfeedback?lang=vi", feedbackFormUrl("vi"))
    }

    @Test
    fun testZHHans() {
        assertEquals("https://mbta.com/androidappfeedback?lang=zh-Hans", feedbackFormUrl("zh-Hans-CN"))
    }

    @Test
    fun testZHHant() {
        assertEquals("https://mbta.com/androidappfeedback?lang=zh-Hant", feedbackFormUrl("zh-Hant-TW"))
    }

    @Test
    fun testUnknown() {
        assertEquals("https://mbta.com/androidappfeedback", feedbackFormUrl("tok"))
    }
}
