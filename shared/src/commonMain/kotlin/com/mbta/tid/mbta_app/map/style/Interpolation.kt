package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

internal sealed interface Interpolation : MapboxStyleObject {
    data object Linear : Interpolation {
        override fun asJson() = buildJsonArray { add("linear") }
    }

    data class Exponential(val base: Number) : Interpolation {
        override fun asJson() = buildJsonArray {
            add("exponential")
            add(base)
        }
    }

    data class CubicBezier(val x1: Number, val y1: Number, val x2: Number, val y2: Number) :
        Interpolation {
        override fun asJson() = buildJsonArray {
            add("cubic-bezier")
            add(x1)
            add(y1)
            add(x2)
            add(y2)
        }
    }
}
