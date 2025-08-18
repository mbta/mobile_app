package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkStateTest {
    @Test
    fun `matches root`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(DeepLinkState.None, DeepLinkState.from(variant.backendRoot))
    }

    @Test
    fun `returns null on unknown path`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertNull(DeepLinkState.from("${variant.backendRoot}/some-unknown-path"))
    }
}
