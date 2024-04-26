package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mbta.tid.mbta_app.model.LocationType

class StopLayerGenerator(stopLayerTypes: List<LocationType>) {
    val stopLayers = createStopLayers(stopLayerTypes)

    companion object {
        val stopLayerId = "stop-layer"

        fun getStopLayerId(locationType: LocationType) = "$stopLayerId-${locationType.name}"

        fun createStopLayers(stopLayerTypes: List<LocationType>): List<SymbolLayer> =
            stopLayerTypes.map { createStopLayer(it) }

        fun createStopLayer(locationType: LocationType): SymbolLayer {
            val layerId = getStopLayerId(locationType)
            val sourceId = StopSourceGenerator.getStopSourceId(locationType)
            val stopLayer =
                SymbolLayer(layerId, sourceId)
                    .iconImage(StopIcons.getStopLayerIcon(locationType))
                    .iconAllowOverlap(true)
                    .minZoom(StopIcons.stopZoomThreshold - 1)
                    .iconOpacity(0.0)
                    .iconOpacityTransition {
                        duration(1)
                        delay(0)
                    }
            return stopLayer
        }
    }
}
