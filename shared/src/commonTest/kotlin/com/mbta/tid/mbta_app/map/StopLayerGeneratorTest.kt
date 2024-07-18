package com.mbta.tid.mbta_app.map

import kotlin.test.Test
import kotlin.test.assertEquals

class StopLayerGeneratorTest {
    @Test
    fun `stop layers are created`() {
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
}
