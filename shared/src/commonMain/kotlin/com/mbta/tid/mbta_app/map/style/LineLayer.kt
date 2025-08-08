package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

public data class LineLayer
internal constructor(override val id: String, override val source: String) : Layer() {
    override val type: LayerType = LayerType.Line
    override var filter: Exp<Boolean>? = null

    var lineColor: Exp<Color>? = null
    var lineDasharray: List<Double>? = null
    var lineJoin: LineJoin? = null
    var lineOffset: Exp<Number>? = null
    var lineSortKey: Exp<Number>? = null
    var lineWidth: Exp<Number>? = null

    override fun layoutAsJson() = buildJsonObject {
        lineJoin?.let { put("line-join", it) }
        lineSortKey?.let { put("line-sort-key", it) }
    }

    override fun paintAsJson() = buildJsonObject {
        lineColor?.let { put("line-color", it) }
        lineDasharray?.let { put("line-dasharray", JsonArray(it.map(::JsonPrimitive))) }
        lineOffset?.let { put("line-offset", it) }
        lineWidth?.let { put("line-width", it) }
    }
}
