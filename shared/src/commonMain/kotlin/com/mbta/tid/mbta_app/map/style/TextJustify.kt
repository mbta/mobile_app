package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

public enum class TextJustify : MapboxStyleObject {
    AUTO,
    LEFT,
    CENTER,
    RIGHT;

    override fun asJson(): JsonPrimitive =
        when (this) {
            AUTO -> JsonPrimitive("auto")
            LEFT -> JsonPrimitive("left")
            CENTER -> JsonPrimitive("center")
            RIGHT -> JsonPrimitive("right")
        }
}
