package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun `compareSubwayFirst returns -1  when first is subway and second is not`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var busRoute = route { type = RouteType.BUS }

        assertEquals(-1, Route.subwayFirstComparator.compare(subwayRoute, busRoute))
    }

    @Test
    fun `compareSubwayFirst returns 0  when both are subway`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var lightRailRoute = route { type = RouteType.LIGHT_RAIL }

        assertEquals(0, Route.subwayFirstComparator.compare(subwayRoute, lightRailRoute))
    }

    @Test
    fun `compareSubwayFirst returns 0  when both are not subway`() {
        var busRoute = route { type = RouteType.BUS }

        var crRoute = route { type = RouteType.COMMUTER_RAIL }

        assertEquals(0, Route.subwayFirstComparator.compare(busRoute, crRoute))
    }

    @Test
    fun `compareSubwayFirst returns 1  when only second is subway`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var busRoute = route { type = RouteType.BUS }

        assertEquals(1, Route.subwayFirstComparator.compare(busRoute, subwayRoute))
    }
}
