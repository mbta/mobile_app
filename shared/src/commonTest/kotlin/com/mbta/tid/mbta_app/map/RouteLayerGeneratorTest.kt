package com.mbta.tid.mbta_app.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class RouteLayerGeneratorTest {
    @Test
    fun `route layers are created`() {
        val routeLayers = RouteLayerGenerator.routeLayers

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
        val routeLayers = RouteLayerGenerator.routeLayers

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
        val routeLayers = RouteLayerGenerator.routeLayers

        val baseRouteLayer = routeLayers[0]
        assertEquals(
            buildJsonArray {
                add("get")
                add(RouteFeaturesBuilder.propRouteColor)
            },
            baseRouteLayer.lineColor!!.asJson()
        )
    }

    @Test
    fun `sort key comes from data`() {
        val routeLayers = RouteLayerGenerator.routeLayers

        val baseRouteLayer = routeLayers[0]
        assertEquals(
            buildJsonArray {
                add("get")
                add(RouteFeaturesBuilder.propRouteSortKey)
            },
            baseRouteLayer.lineSortKey!!.asJson()
        )
    }
}
