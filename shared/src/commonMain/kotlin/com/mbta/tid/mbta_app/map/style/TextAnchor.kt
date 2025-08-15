package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

public enum class TextAnchor : MapboxStyleObject {
    CENTER,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    override fun asJson(): JsonPrimitive =
        when (this) {
            CENTER -> JsonPrimitive("center")
            LEFT -> JsonPrimitive("left")
            RIGHT -> JsonPrimitive("right")
            TOP -> JsonPrimitive("top")
            BOTTOM -> JsonPrimitive("bottom")
            TOP_LEFT -> JsonPrimitive("top-left")
            TOP_RIGHT -> JsonPrimitive("top-right")
            BOTTOM_LEFT -> JsonPrimitive("bottom-left")
            BOTTOM_RIGHT -> JsonPrimitive("bottom-right")
        }
}
