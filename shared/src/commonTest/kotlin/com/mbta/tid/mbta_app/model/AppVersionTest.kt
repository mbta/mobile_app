package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun `comparison works`() {
        assertEquals(AppVersion(1u, 2u, 3u), AppVersion(1u, 2u, 3u))
        assertTrue(AppVersion(1u, 2u, 3u) < AppVersion(1u, 2u, 4u))
        assertTrue(AppVersion(1u, 2u, 3u) < AppVersion(1u, 3u, 0u))
        assertTrue(AppVersion(1u, 2u, 3u) < AppVersion(2u, 0u, 0u))
    }

    @Test
    fun `toString works`() {
        assertEquals("10.9.87", AppVersion(10u, 9u, 87u).toString())
        assertEquals("3.0.0", AppVersion(3u, 0u, 0u).toString())
    }

    @Test
    fun `parse works`() {
        assertEquals(AppVersion(0u, 0u, 0u), AppVersion.parse("0.0.0"))
        assertEquals(AppVersion(1u, 2u, 3u), AppVersion.parse("1.2.3"))
        assertEquals(AppVersion(12u, 345u, 6789u), AppVersion.parse("12.345.6789"))
        assertFails { AppVersion.parse("1.2.-3") }
        assertFails { AppVersion.parse("1.0") }
        assertFails { AppVersion.parse("1.2.3-beta9") }
        assertFails { AppVersion.parse("Ceci n'est pas une version") }
    }
}
