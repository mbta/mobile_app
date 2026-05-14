package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

public enum class TranslateAnchor : MapboxStyleObject {
    MAP,
    VIEWPORT;

    override fun asJson(): JsonPrimitive =
        when (this) {
            MAP -> JsonPrimitive("map")
            VIEWPORT -> JsonPrimitive("viewport")
        }
}
