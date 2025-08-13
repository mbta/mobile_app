package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.RouteType
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteModeLabelTest {
    @Test
    fun testNameAndType() {
        val name = "1"
        val type = RouteType.BUS

        val label = routeModeLabel(name, type)
        assertEquals(RouteModeLabelType.NameAndType(name, type), label)
    }

    @Test
    fun testNullType() {
        val name = "1"

        val label = routeModeLabel(name, null)
        assertEquals(RouteModeLabelType.NameOnly(name), label)
    }

    @Test
    fun testNullName() {
        val type = RouteType.HEAVY_RAIL

        val label = routeModeLabel(null, type)
        assertEquals(RouteModeLabelType.TypeOnly(type), label)
    }

    @Test
    fun testFerryType() {
        val name = "Hingham/Hull Ferry"
        val type = RouteType.FERRY

        val label = routeModeLabel(name, type)
        assertEquals(RouteModeLabelType.NameOnly(name), label)
    }

    @Test
    fun testEmpty() {
        val label = routeModeLabel(null, null)
        assertEquals(RouteModeLabelType.Empty, label)
    }
}
