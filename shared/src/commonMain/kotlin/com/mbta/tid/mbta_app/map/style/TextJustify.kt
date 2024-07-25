package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

enum class TextJustify : MapboxStyleObject {
    AUTO,
    LEFT,
    CENTER,
    RIGHT;

    override fun asJson() =
        when (this) {
            AUTO -> JsonPrimitive("auto")
            LEFT -> JsonPrimitive("left")
            CENTER -> JsonPrimitive("center")
            RIGHT -> JsonPrimitive("right")
        }
}
