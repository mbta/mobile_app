package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.evaluate
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class StopLayerGeneratorTest {
    @Test
    fun `stop layers are created`() = runBlocking {
        val stopLayers = StopLayerGenerator.createStopLayers(ColorPalette.light)

        assertEquals(11, stopLayers.size)
        assertEquals(StopLayerGenerator.stopTouchTargetLayerId, stopLayers[0].id)
        assertEquals(StopLayerGenerator.busLayerId, stopLayers[1].id)
        assertEquals(StopLayerGenerator.busAlertLayerId, stopLayers[2].id)
        assertEquals(StopLayerGenerator.stopLayerId, stopLayers[3].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(0), stopLayers[4].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(1), stopLayers[5].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(2), stopLayers[6].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(0), stopLayers[7].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(1), stopLayers[8].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(2), stopLayers[9].id)
        assertEquals(StopLayerGenerator.stopLayerSelectedPinId, stopLayers[10].id)
    }

    @Test
    fun `North Station with alerts has correct properties`() = runBlocking {
        val northStation = TestData.getStop("place-north")
        val routes =
            mapOf(
                MapStopRoute.ORANGE to listOf(TestData.getRoute("Orange")),
                MapStopRoute.GREEN to
                    listOf(
                        TestData.getRoute("Green-D"),
                        TestData.getRoute("Green-E"),
                    ),
                MapStopRoute.COMMUTER to
                    listOf(
                        TestData.getRoute("CR-Fitchburg"),
                        TestData.getRoute("CR-Haverhill"),
                        TestData.getRoute("CR-Lowell"),
                        TestData.getRoute("CR-Newburyport")
                    )
            )

        val mapStops =
            mapOf(
                northStation.id to
                    MapStop(
                        stop = northStation,
                        routes = routes,
                        routeTypes =
                            listOf(MapStopRoute.ORANGE, MapStopRoute.GREEN, MapStopRoute.COMMUTER),
                        isTerminal = true,
                        alerts =
                            mapOf(
                                MapStopRoute.ORANGE to StopAlertState.Shuttle,
                                MapStopRoute.GREEN to StopAlertState.Suspension,
                                MapStopRoute.COMMUTER to StopAlertState.Normal,
                                MapStopRoute.BUS to StopAlertState.Normal
                            )
                    )
            )

        val feature =
            StopFeaturesBuilder.buildCollection(StopSourceData(), mapStops, emptyList())
                .features
                .single()

        val stopLayers = StopLayerGenerator.createStopLayers(ColorPalette.light)

        val busLayer = stopLayers[1]
        val busAlertLayer = stopLayers[2]
        val stopLayer = stopLayers[3]
        val transferLayer0 = stopLayers[4]
        val transferLayer1 = stopLayers[5]
        val transferLayer2 = stopLayers[6]
        val alertLayer0 = stopLayers[7]
        val alertLayer1 = stopLayers[8]
        val alertLayer2 = stopLayers[9]

        assertEquals("", busLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busLayer.textField!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busAlertLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))

        assertEquals(
            "map-stop-container-wide-3",
            stopLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "North Station",
            stopLayer.textField!!.evaluate(feature.properties, zoom = 14.0)
        )

        assertEquals(
            "map-stop-wide-ORANGE",
            transferLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "map-stop-wide-GREEN",
            transferLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "map-stop-wide-COMMUTER",
            transferLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )

        assertEquals(
            "alert-small-orange-shuttle",
            alertLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "alert-small-green-suspension",
            alertLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals("", alertLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0))
    }
}
