package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.layers.getCachedLayerProperties
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteLayerGeneratorTest {
    @Test
    fun testRouteLayersAreCreated() {
        val routeLayerGenerator =
            RouteLayerGenerator(
                MapTestDataHelper.routeResponse,
                mapOf(
                    MapTestDataHelper.routeRed.id to MapTestDataHelper.routeRed,
                    MapTestDataHelper.routeOrange.id to MapTestDataHelper.routeOrange
                )
            )
        val routeLayers = routeLayerGenerator.routeLayers

        // 2 layers per route - alerting & non-alerting
        assertEquals(4, routeLayers.size)
        val redRouteLayer =
            routeLayers.find {
                it.layerId == RouteLayerGenerator.getRouteLayerId(MapTestDataHelper.routeRed.id)
            }
        assertNotNull(redRouteLayer)
        assertEquals(
            "#${MapTestDataHelper.routeRed.color}",
            redRouteLayer!!
                .getCachedLayerProperties()
                .jsonObject["line-color"]!!
                .jsonPrimitive
                .content
        )

        val alertingRedLayer =
            routeLayers.find {
                it.layerId ==
                    RouteLayerGenerator.getRouteLayerId("${MapTestDataHelper.routeRed.id}-alerting")
            }
        assertNotNull(alertingRedLayer)
        assertNotNull(
            alertingRedLayer!!.getCachedLayerProperties().jsonObject["line-dasharray"]!!.jsonArray
        )
    }
}
