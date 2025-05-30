package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive

class RouteLayerGeneratorTest {
    @Test
    fun `route layers are created`(): Unit = runBlocking {
        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(
                MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                MapTestDataHelper.global,
                ColorPalette.light,
            )

        assertEquals(
            listOf(
                "route-layer-Orange",
                "route-layer-Red",
                "route-layer-Orange-alerting-bg",
                "route-layer-Orange-shuttled",
                "route-layer-Orange-suspended",
                "route-layer-Red-alerting-bg",
                "route-layer-Red-shuttled",
                "route-layer-Red-suspended",
            ),
            routeLayers.map { it.id },
        )

        for (alertingRouteLayer in
            routeLayers.filter { it.id.endsWith("shuttled") || it.id.endsWith("suspended") }) {
            assertNotNull(alertingRouteLayer.lineDasharray)
        }
    }

    @Test
    fun `layers have offset`(): Unit = runBlocking {
        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(
                MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                MapTestDataHelper.global,
                ColorPalette.light,
            )

        for (layer in routeLayers) {
            assertNotNull(layer.lineOffset)
        }
    }

    @Test
    fun `base layer color matches data`() = runBlocking {
        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(
                MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                MapTestDataHelper.global,
                ColorPalette.light,
            )

        for (route in listOf(MapTestDataHelper.routeRed, MapTestDataHelper.routeOrange)) {
            assertEquals(
                JsonPrimitive("#${route.color}"),
                routeLayers
                    .first { it.id == RouteLayerGenerator.getRouteLayerId(route.id) }
                    .lineColor!!
                    .asJson(),
            )
        }
    }

    @Test
    fun `uses provided colors`() = parametricTest {
        val colorPalette = anyOf(ColorPalette.light, ColorPalette.dark)

        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(
                MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                MapTestDataHelper.global,
                colorPalette,
            )

        for (suspendedLayer in routeLayers.filter { it.id.endsWith("-suspended") }) {
            assertEquals(
                JsonPrimitive(colorPalette.deemphasized),
                suspendedLayer.lineColor?.asJson(),
            )
        }

        for (alertBackgroundLayer in routeLayers.filter { it.id.endsWith("-bg") }) {
            assertEquals(
                JsonPrimitive(colorPalette.fill3),
                alertBackgroundLayer.lineColor?.asJson(),
            )
        }
    }
}
