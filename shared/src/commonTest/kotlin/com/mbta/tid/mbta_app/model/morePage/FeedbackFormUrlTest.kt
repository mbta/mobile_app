package com.mbta.tid.mbta_app.model.morePage

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedFeedbackFormUrlTest {
    @Test
    fun testEN() {
        assertEquals("baseUrl", localizedFeedbackFormUrl("baseUrl","en"))
    }

    @Test
    fun testES() {
        assertEquals("baseUrl?lang=es-US", localizedFeedbackFormUrl("baseUrl","es"))
    }

    @Test
    fun testHT() {
        assertEquals("baseUrl", localizedFeedbackFormUrl("baseUrl", "ht"))
    }

    @Test
    fun testPTBR() {
        assertEquals("baseUrl?lang=pt-BR", localizedFeedbackFormUrl("baseUrl","pt-BR"))
    }

    @Test
    fun testVI() {
        assertEquals("baseUrl?lang=vi", localizedFeedbackFormUrl("baseUrl","vi"))
    }

    @Test
    fun testZHHans() {
        assertEquals("baseUrl?lang=zh-Hans", localizedFeedbackFormUrl("baseUrl","zh-Hans-CN"))
    }

    @Test
    fun testZHHant() {
        assertEquals("baseUrl?lang=zh-Hant", localizedFeedbackFormUrl("baseUrl","zh-Hant-TW"))
    }

    @Test
    fun testUnknown() {
        assertEquals("baseUrl", localizedFeedbackFormUrl("baseUrl","tok"))
    }
}
