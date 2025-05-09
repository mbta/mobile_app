package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import com.mbta.tid.mbta_app.map.style.TextAnchor
import com.mbta.tid.mbta_app.map.style.TextJustify
import com.mbta.tid.mbta_app.map.style.downcastToColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StopLayerGenerator {
    val maxTransferLayers = 3
    val stopZoomThreshold = 11.0
    val busStopZoomThreshold = 12.0

    val stopLayerId = "stop-layer"
    val stopTouchTargetLayerId = "${stopLayerId}-touch-target"
    val stopLayerSelectedPinId = "${stopLayerId}-selected-pin"

    val busLayerId = "${stopLayerId}-bus"
    val busAlertLayerId = "${stopLayerId}-bus-alert"

    fun getAlertLayerId(index: Int) = "${stopLayerId}-alert-${index}"

    fun getTransferLayerId(index: Int) = "${stopLayerId}-transfer-${index}"

    suspend fun createStopLayers(colorPalette: ColorPalette): List<SymbolLayer> {
        return withContext(Dispatchers.Default) {
            val sourceId = StopFeaturesBuilder.stopSourceId

            val stopLayer = createStopLayer(id = stopLayerId, colorPalette = colorPalette)

            val stopTouchTargetLayer = SymbolLayer(id = stopTouchTargetLayerId, source = sourceId)
            stopTouchTargetLayer.iconImage = Exp.image(Exp(StopIcons.stopDummyIcon))
            stopTouchTargetLayer.iconPadding = 22.0
            includeSharedProps(stopTouchTargetLayer, forBus = false)

            val stopSelectedPinLayer = SymbolLayer(id = stopLayerSelectedPinId, source = sourceId)
            stopSelectedPinLayer.iconImage =
                Exp.case(
                    MapExp.selectedExp to Exp.image(Exp(StopIcons.stopPinIcon)),
                    Exp.image(Exp("")),
                )
            stopSelectedPinLayer.textField =
                Exp.case(MapExp.selectedExp to Exp.get(StopFeaturesBuilder.propNameKey), Exp(""))
            includedDefaultTextProps(stopSelectedPinLayer, colorPalette)
            stopSelectedPinLayer.iconOffset = offsetPinValue()
            stopTouchTargetLayer.filter =
                Exp.ge(
                    Exp.zoom(),
                    Exp.case(MapExp.isBusExp to Exp(busStopZoomThreshold), Exp(stopZoomThreshold)),
                )
            includeSharedProps(stopSelectedPinLayer, forBus = false)

            val transferLayers =
                (0.rangeUntil(maxTransferLayers)).map { index ->
                    val transferLayer =
                        SymbolLayer(id = getTransferLayerId(index), source = sourceId)
                    transferLayer.iconImage = (StopIcons.getTransferLayerIcon(index))
                    transferLayer.iconOffset = offsetTransferValue(index)
                    includeSharedProps(transferLayer, forBus = false)

                    return@map transferLayer
                }

            val alertLayers =
                (0.rangeUntil(maxTransferLayers)).map { index ->
                    createAlertLayer(id = getAlertLayerId(index), index)
                }

            val busLayer = createStopLayer(id = busLayerId, forBus = true, colorPalette)
            val busAlertLayer = createAlertLayer(id = busAlertLayerId, forBus = true)

            listOf(stopTouchTargetLayer, busLayer, busAlertLayer, stopLayer) +
                transferLayers +
                alertLayers +
                listOf(stopSelectedPinLayer)
        }
    }

    fun createAlertLayer(id: String, index: Int = 0, forBus: Boolean = false): SymbolLayer {
        val alertLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        alertLayer.iconImage = AlertIcons.getAlertLayerIcon(index, forBus = forBus)
        alertLayer.iconOffset = offsetAlertValue(index)
        alertLayer.iconAllowOverlap = true
        includeSharedProps(alertLayer, forBus)

        return alertLayer
    }

    fun createStopLayer(
        id: String,
        forBus: Boolean = false,
        colorPalette: ColorPalette,
    ): SymbolLayer {
        val stopLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        stopLayer.iconImage = (StopIcons.getStopLayerIcon(forBus = forBus))
        stopLayer.textField = (MapExp.stopLabelTextExp(forBus = forBus))
        includedDefaultTextProps(stopLayer, colorPalette)
        stopLayer.textAllowOverlap = false

        includeSharedProps(stopLayer, forBus)
        return stopLayer
    }

    fun includedDefaultTextProps(layer: SymbolLayer, colorPalette: ColorPalette) {
        layer.textColor = Exp(colorPalette.text).downcastToColor()
        layer.textFont = listOf("Inter Regular")
        layer.textHaloColor = Exp(colorPalette.fill3).downcastToColor()
        layer.textHaloWidth = 2.0
        layer.textSize = 13.0
        layer.textVariableAnchor =
            listOf(TextAnchor.RIGHT, TextAnchor.BOTTOM, TextAnchor.TOP, TextAnchor.LEFT)
        layer.textJustify = TextJustify.AUTO
        layer.textAllowOverlap = true
        layer.textOptional = true
        layer.textOffset = MapExp.labelOffsetExp
    }

    fun includeSharedProps(layer: SymbolLayer, forBus: Boolean) {
        layer.iconSize = MapExp.selectedSizeExp

        layer.iconAllowOverlap = true
        layer.minZoom = if (forBus) busStopZoomThreshold else stopZoomThreshold
        layer.symbolSortKey = Exp.get(StopFeaturesBuilder.propSortOrderKey)
    }

    fun offsetAlertValue(index: Int): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetAlertExp(closeZoom = false, index),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetAlertExp(closeZoom = true, index),
        )
    }

    fun offsetTransferValue(index: Int): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetTransferExp(closeZoom = false, index),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetTransferExp(closeZoom = true, index),
        )
    }

    fun offsetPinValue(): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetPinExp(closeZoom = false),
            Exp(MapDefaults.closeZoomThreshold) to MapExp.offsetPinExp(closeZoom = true),
        )
    }
}
