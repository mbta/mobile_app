package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {
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
            longName = "Mattapan Line"
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
        assertEquals("Mattapan Line", light.label)
        assertEquals("Framingham / Worcester Line", commuter.label)
        assertEquals("1", bus.label)
        assertEquals("Charlestown Ferry", ferry.label)
    }
}
