package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun `subwayFirstComparator returns -1  when first is subway and second is not`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var busRoute = route { type = RouteType.BUS }

        assertEquals(-1, Route.subwayFirstComparator.compare(subwayRoute, busRoute))
    }

    @Test
    fun `subwayFirstComparator returns 0  when both are subway`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var lightRailRoute = route { type = RouteType.LIGHT_RAIL }

        assertEquals(0, Route.subwayFirstComparator.compare(subwayRoute, lightRailRoute))
    }

    @Test
    fun `subwayFirstComparator returns 0  when both are not subway`() {
        var busRoute = route { type = RouteType.BUS }

        var crRoute = route { type = RouteType.COMMUTER_RAIL }

        assertEquals(0, Route.subwayFirstComparator.compare(busRoute, crRoute))
    }

    @Test
    fun `subwayFirstComparator returns 1  when only second is subway`() {
        var subwayRoute = route { type = RouteType.HEAVY_RAIL }

        var busRoute = route { type = RouteType.BUS }

        assertEquals(1, Route.subwayFirstComparator.compare(busRoute, subwayRoute))
    }

    @Test
    fun `relevanceComparator sorts pinned then subway then rest in original order`() {
        val pinnedSubway: Route = route {
            type = RouteType.HEAVY_RAIL
            id = "pinned_subway"
        }
        val subway = route {
            type = RouteType.HEAVY_RAIL
            id = "subway"
        }

        val pinnedBus = route {
            type = RouteType.BUS
            id = "pinned_bus"
        }
        val bus = route {
            type = RouteType.BUS
            id = "bus"
        }

        val cr = route {
            type = RouteType.COMMUTER_RAIL
            id = "cr"
        }

        assertEquals(
            listOf(pinnedSubway, pinnedBus, subway, cr, bus),
            listOf(cr, pinnedSubway, bus, pinnedBus, subway)
                .sortedWith(Route.relevanceComparator(setOf(pinnedSubway.id, pinnedBus.id)))
        )
    }

    @Test
    fun `label uses short or long name depending on mode`() {
        val heavy = route {
            type = RouteType.HEAVY_RAIL
            shortName = "Red"
            longName = "Red Line"
        }

        val light = route {
            type = RouteType.LIGHT_RAIL
            shortName = "M"
            longName = "Mattapan Trolley"
        }

        val commuter = route {
            type = RouteType.COMMUTER_RAIL
            longName = "Framingham/Worcester Line"
        }

        val bus = route {
            type = RouteType.BUS
            longName = "Harvard Square - Nubian Station"
            shortName = "1"
        }

        val ferry = route {
            type = RouteType.FERRY
            longName = "Charlestown Ferry"
        }

        assertEquals("Red Line", heavy.label)
        assertEquals("Mattapan Trolley", light.label)
        assertEquals("Framingham / Worcester Line", commuter.label)
        assertEquals("1", bus.label)
        assertEquals("Charlestown Ferry", ferry.label)
    }
}
