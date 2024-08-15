package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

enum class LineJoin : MapboxStyleObject {
    Bevel,
    Round,
    Miter,
    None;

    override fun asJson() =
        when (this) {
            Bevel -> JsonPrimitive("bevel")
            Round -> JsonPrimitive("round")
            Miter -> JsonPrimitive("miter")
            None -> JsonPrimitive("none")
        }
}
