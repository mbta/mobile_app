package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteTypeTest {

    @Test
    fun `isSubway only true when light or heavy rail`() {
        assertTrue { RouteType.isSubway(RouteType.LIGHT_RAIL) }
        assertTrue { RouteType.isSubway(RouteType.HEAVY_RAIL) }
        assertFalse { RouteType.isSubway(RouteType.COMMUTER_RAIL) }
        assertFalse { RouteType.isSubway(RouteType.BUS) }
        assertFalse { RouteType.isSubway(RouteType.FERRY) }
    }
}
