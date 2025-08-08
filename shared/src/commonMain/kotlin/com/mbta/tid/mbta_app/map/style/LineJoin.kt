package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

public enum class LineJoin : MapboxStyleObject {
    Bevel,
    Round,
    Miter,
    None;

    override fun asJson(): JsonPrimitive =
        when (this) {
            Bevel -> JsonPrimitive("bevel")
            Round -> JsonPrimitive("round")
            Miter -> JsonPrimitive("miter")
            None -> JsonPrimitive("none")
        }
}
