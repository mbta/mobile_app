package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

public enum class LayerType : MapboxStyleObject {
    Line,
    Symbol;

    override fun asJson(): JsonPrimitive =
        when (this) {
            Line -> JsonPrimitive("line")
            Symbol -> JsonPrimitive("symbol")
        }
}
