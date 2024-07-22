package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

enum class LayerType : MapboxStyleObject {
    Line,
    Symbol;

    override fun asJson() =
        when (this) {
            Line -> JsonPrimitive("line")
            Symbol -> JsonPrimitive("symbol")
        }
}
