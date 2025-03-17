package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.layers.generated.SymbolLayer as MapboxSymbolLayer
import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxBridgeTest {
    @Test
    fun `symbol layer gets translated properly`() = runBlocking {
        val symbolLayer = SymbolLayer(id = "symbol-layer", source = "stop-source")
        symbolLayer.textField = Exp("this is text")

        val bridgedSymbolLayer = symbolLayer.toMapbox()

        val mapboxSymbolLayer =
            MapboxSymbolLayer("symbol-layer", "stop-source").textField("this is text")

        assertEquals(mapboxSymbolLayer.toString(), bridgedSymbolLayer.toString())
    }
}
