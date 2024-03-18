package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteTypeTest {

    @Test
    fun `isSubway only true when light or heavy rail`() {
        assertTrue { RouteType.LIGHT_RAIL.isSubway() }
        assertTrue { RouteType.HEAVY_RAIL.isSubway() }
        assertFalse { RouteType.COMMUTER_RAIL.isSubway() }
        assertFalse { RouteType.BUS.isSubway() }
        assertFalse { RouteType.FERRY.isSubway() }
    }
}
