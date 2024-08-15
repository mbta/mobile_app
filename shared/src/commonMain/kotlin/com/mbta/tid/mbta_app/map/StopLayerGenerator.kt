package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import com.mbta.tid.mbta_app.map.style.TextAnchor
import com.mbta.tid.mbta_app.map.style.TextJustify
import com.mbta.tid.mbta_app.map.style.downcastToColor

object StopLayerGenerator {
    val maxTransferLayers = 3
    val stopZoomThreshold = 8.0

    val stopLayerId = "stop-layer"
    val stopTouchTargetLayerId = "${stopLayerId}-touch-target"
    val stopLayerSelectedPinId = "${stopLayerId}-selected-pin"

    val busLayerId = "${stopLayerId}-bus"
    val busAlertLayerId = "${stopLayerId}-bus-alert"

    fun getAlertLayerId(index: Int) = "${stopLayerId}-alert-${index}"

    fun getTransferLayerId(index: Int) = "${stopLayerId}-transfer-${index}"

    fun createStopLayers(colorPalette: ColorPalette): List<SymbolLayer> {
        val sourceId = StopFeaturesBuilder.stopSourceId
        val stopLayer = createStopLayer(id = stopLayerId, colorPalette = colorPalette)
        val busLayer = createStopLayer(id = busLayerId, forBus = true, colorPalette)

        val stopTouchTargetLayer = SymbolLayer(id = stopTouchTargetLayerId, source = sourceId)
        stopTouchTargetLayer.iconImage = Exp.image(Exp(StopIcons.stopDummyIcon))
        stopTouchTargetLayer.iconPadding = 22.0
        includeSharedProps(stopTouchTargetLayer)

        val stopSelectedPinLayer = SymbolLayer(id = stopLayerSelectedPinId, source = sourceId)
        stopSelectedPinLayer.iconImage =
            Exp.case(
                MapExp.selectedExp to Exp.image(Exp(StopIcons.stopPinIcon)),
                Exp.image(Exp(""))
            )
        stopSelectedPinLayer.iconOffset = offsetPinValue()
        includeSharedProps(stopSelectedPinLayer)

        val transferLayers =
            (0.rangeUntil(maxTransferLayers)).map { index ->
                val transferLayer = SymbolLayer(id = getTransferLayerId(index), source = sourceId)
                transferLayer.iconImage = (StopIcons.getTransferLayerIcon(index))
                transferLayer.iconOffset = offsetTransferValue(index)
                includeSharedProps(transferLayer)

                return@map transferLayer
            }

        val alertLayers =
            (0.rangeUntil(maxTransferLayers)).map { index ->
                createAlertLayer(id = getAlertLayerId(index), index)
            }

        val busAlertLayer = createAlertLayer(id = busAlertLayerId, forBus = true)

        return listOf(stopTouchTargetLayer, busLayer, busAlertLayer, stopLayer) +
            transferLayers +
            alertLayers +
            listOf(stopSelectedPinLayer)
    }

    fun createAlertLayer(id: String, index: Int = 0, forBus: Boolean = false): SymbolLayer {
        val alertLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        alertLayer.iconImage = AlertIcons.getAlertLayerIcon(index, forBus = forBus)
        alertLayer.iconOffset = offsetAlertValue(index)
        alertLayer.iconAllowOverlap = true
        includeSharedProps(alertLayer)

        return alertLayer
    }

    fun createStopLayer(
        id: String,
        forBus: Boolean = false,
        colorPalette: ColorPalette
    ): SymbolLayer {
        val stopLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        stopLayer.iconImage = (StopIcons.getStopLayerIcon(forBus = forBus))
        stopLayer.textField = (MapExp.stopLabelTextExp(forBus = forBus))

        stopLayer.textColor = Exp(colorPalette.text).downcastToColor()
        stopLayer.textFont = listOf("Inter Regular")
        stopLayer.textHaloColor = Exp(colorPalette.fill3).downcastToColor()
        stopLayer.textHaloWidth = 2.0
        stopLayer.textSize = 13.0
        stopLayer.textVariableAnchor =
            listOf(TextAnchor.RIGHT, TextAnchor.BOTTOM, TextAnchor.TOP, TextAnchor.LEFT)
        stopLayer.textJustify = TextJustify.AUTO
        stopLayer.textAllowOverlap = true
        stopLayer.textOptional = true
        stopLayer.textOffset = MapExp.labelOffsetExp

        includeSharedProps(stopLayer)
        return stopLayer
    }

    fun includeSharedProps(layer: SymbolLayer) {
        layer.iconSize = MapExp.selectedSizeExp

        layer.iconAllowOverlap = true
        layer.minZoom = stopZoomThreshold
        layer.symbolSortKey = Exp.get(StopFeaturesBuilder.propSortOrderKey)
    }

    fun offsetAlertValue(index: Int): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetAlertExp(closeZoom = false, index),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetAlertExp(closeZoom = true, index)
        )
    }

    fun offsetTransferValue(index: Int): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetTransferExp(closeZoom = false, index),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetTransferExp(closeZoom = true, index)
        )
    }

    fun offsetPinValue(): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetPinExp(closeZoom = false),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetPinExp(closeZoom = true)
        )
    }
}
