package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.layers.getCachedLayerProperties
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.LocationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StopLayerGeneratorTest {
    @Test
    fun testStopLayersAreCreated() {
        val stopLayerGenerator = StopLayerGenerator(listOf(LocationType.STOP, LocationType.STATION))
        val stopLayers = stopLayerGenerator.stopLayers

        assertEquals(2, stopLayers.size)
        val stationLayer =
            stopLayers.first {
                it.layerId == StopLayerGenerator.getStopLayerId(LocationType.STATION)
            }
        assertNotNull(stationLayer)
        assertEquals(
            json.parseToJsonElement(StopIcons.getStopLayerIcon(LocationType.STATION).toJson()),
            stationLayer.getCachedLayerProperties()["icon-image"]
        )
    }
}
