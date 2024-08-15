package com.mbta.tid.mbta_app.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class RouteLayerGeneratorTest {
    @Test
    fun `route layers are created`() {
        val routeLayers = RouteLayerGenerator.createAllRouteLayers(ColorPalette.light)

        assertEquals(4, routeLayers.size)
        val baseRouteLayer = routeLayers[0]
        assertEquals(RouteLayerGenerator.routeLayerId, baseRouteLayer.id)
        val alertingBgLayer = routeLayers[1]
        assertEquals(RouteLayerGenerator.alertingBgRouteLayerId, alertingBgLayer.id)
        val shuttledLayer = routeLayers[2]
        assertEquals(RouteLayerGenerator.shuttledRouteLayerId, shuttledLayer.id)
        val suspendedLayer = routeLayers[3]
        assertEquals(RouteLayerGenerator.suspendedRouteLayerId, suspendedLayer.id)

        assertNotNull(shuttledLayer.lineDasharray)
        assertNotNull(suspendedLayer.lineDasharray)
    }

    @Test
    fun `layers have offset`() {
        val routeLayers = RouteLayerGenerator.createAllRouteLayers(ColorPalette.light)

        assertEquals(4, routeLayers.size)
        val baseRouteLayer = routeLayers[0]
        assertEquals(RouteLayerGenerator.routeLayerId, baseRouteLayer.id)
        val alertingBgLayer = routeLayers[1]
        assertEquals(RouteLayerGenerator.alertingBgRouteLayerId, alertingBgLayer.id)
        val shuttledLayer = routeLayers[2]
        assertEquals(RouteLayerGenerator.shuttledRouteLayerId, shuttledLayer.id)
        val suspendedLayer = routeLayers[3]
        assertEquals(RouteLayerGenerator.suspendedRouteLayerId, suspendedLayer.id)

        assertNotNull(shuttledLayer.lineOffset)
        assertNotNull(suspendedLayer.lineOffset)
    }

    @Test
    fun `base layer color comes from data`() {
        val routeLayers = RouteLayerGenerator.createAllRouteLayers(ColorPalette.light)

        val baseRouteLayer = routeLayers[0]
        assertEquals(
            buildJsonArray {
                add("get")
                add(RouteFeaturesBuilder.propRouteColor.key)
            },
            baseRouteLayer.lineColor!!.asJson()
        )
    }

    @Test
    fun `sort key comes from data`() {
        val routeLayers = RouteLayerGenerator.createAllRouteLayers(ColorPalette.light)

        val baseRouteLayer = routeLayers[0]
        assertEquals(
            buildJsonArray {
                add("get")
                add(RouteFeaturesBuilder.propRouteSortKey.key)
            },
            baseRouteLayer.lineSortKey!!.asJson()
        )
    }

    @Test
    fun `uses provided colors`() {
        fun checkColorsMatch(colorPalette: ColorPalette) {
            val routeLayers = RouteLayerGenerator.createAllRouteLayers(colorPalette)

            val suspendedLayer =
                routeLayers.find { it.id == RouteLayerGenerator.suspendedRouteLayerId }
            assertNotNull(suspendedLayer)
            assertEquals(
                JsonPrimitive(colorPalette.deemphasized),
                suspendedLayer.lineColor?.asJson()
            )

            val alertBackgroundLayer =
                routeLayers.find { it.id == RouteLayerGenerator.alertingBgRouteLayerId }
            assertNotNull(alertBackgroundLayer)
            assertEquals(
                JsonPrimitive(colorPalette.fill3),
                alertBackgroundLayer.lineColor?.asJson()
            )
        }

        checkColorsMatch(ColorPalette.light)
        checkColorsMatch(ColorPalette.dark)
    }
}
