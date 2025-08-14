package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import com.mbta.tid.mbta_app.map.style.TextAnchor
import com.mbta.tid.mbta_app.map.style.TextJustify
import com.mbta.tid.mbta_app.map.style.downcastToColor
import com.mbta.tid.mbta_app.map.style.downcastToResolvedImage
import com.mbta.tid.mbta_app.model.LocationType

internal object ChildStopLayerGenerator {
    val childStopLayerId = "child-stop-layer"

    val annotationTextZoomThreshold = 19.0

    fun createChildStopLayer(colorPalette: ColorPalette): SymbolLayer {
        var layer =
            SymbolLayer(id = childStopLayerId, source = ChildStopFeaturesBuilder.childStopSourceId)
        layer.iconImage =
            Exp.match(
                    Exp.get(ChildStopFeaturesBuilder.propLocationTypeKey),
                    Exp(LocationType.ENTRANCE_EXIT.name) to Exp(ChildStopIcons.entranceIcon),
                    Exp.Bare.arrayOf(LocationType.BOARDING_AREA.name, LocationType.STOP.name) to
                        Exp(ChildStopIcons.platformIcon),
                    fallback = Exp(""),
                )
                .downcastToResolvedImage()
        layer.textField =
            Exp.step(
                Exp.zoom(),
                Exp(""),
                Exp(annotationTextZoomThreshold) to Exp.get(ChildStopFeaturesBuilder.propNameKey),
            )

        // TODO actually pick a color
        layer.textColor = Exp("#888888").downcastToColor()
        layer.textFont = listOf("Inter Italic")
        layer.textHaloColor = Exp(colorPalette.fill3).downcastToColor()
        layer.textHaloWidth = 2.0
        layer.textSize = 12.0
        layer.textVariableAnchor =
            listOf(TextAnchor.LEFT, TextAnchor.RIGHT, TextAnchor.BOTTOM, TextAnchor.TOP)
        layer.textJustify = TextJustify.AUTO
        layer.textOptional = true
        layer.textRadialOffset = 1.0
        layer.iconAllowOverlap = true
        layer.textAllowOverlap = false
        layer.symbolSortKey = Exp.get(ChildStopFeaturesBuilder.propSortOrderKey)
        layer.minZoom = MapDefaults.closeZoomThreshold
        return layer
    }
}
