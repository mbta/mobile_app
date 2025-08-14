package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public data class SymbolLayer(override val id: String, override val source: String) : Layer() {
    override val type: LayerType = LayerType.Symbol
    override var filter: Exp<Boolean>? = null

    var iconAllowOverlap: Boolean? = null
    var iconImage: Exp<ResolvedImage>? = null
    var iconOffset: Exp<List<Number>>? = null
    var iconPadding: Double? = null
    var iconSize: Exp<Number>? = null

    var symbolSortKey: Exp<Number>? = null

    var textAllowOverlap: Boolean? = null
    var textColor: Exp<Color>? = null
    var textField: Exp<String>? = null
    var textFont: List<String>? = null
    var textHaloColor: Exp<Color>? = null
    var textHaloWidth: Double? = null
    var textJustify: TextJustify? = null
    var textOffset: Exp<List<Number>>? = null
    var textOptional: Boolean? = null
    var textRadialOffset: Double? = null
    var textSize: Double? = null
    var textVariableAnchor: List<TextAnchor>? = null

    override fun layoutAsJson() = buildJsonObject {
        iconAllowOverlap?.let { put("icon-allow-overlap", it) }
        iconImage?.let { put("icon-image", it) }
        iconOffset?.let { put("icon-offset", it) }
        iconPadding?.let { put("icon-padding", it) }
        iconSize?.let { put("icon-size", it) }

        symbolSortKey?.let { put("symbol-sort-key", it) }

        textAllowOverlap?.let { put("text-allow-overlap", it) }
        textField?.let { put("text-field", it) }
        textFont?.let { put("text-font", JsonArray(it.map(::JsonPrimitive))) }
        textJustify?.let { put("text-justify", it) }
        textOffset?.let { put("text-offset", it) }
        textOptional?.let { put("text-optional", it) }
        textRadialOffset?.let { put("text-radial-offset", it) }
        textSize?.let { put("text-size", it) }
        textVariableAnchor?.let {
            put("text-variable-anchor", JsonArray(it.map(MapboxStyleObject::asJson)))
        }
    }

    override fun paintAsJson() = buildJsonObject {
        textColor?.let { put("text-color", it) }
        textHaloColor?.let { put("text-halo-color", it) }
        textHaloWidth?.let { put("text-halo-width", it) }
    }
}
