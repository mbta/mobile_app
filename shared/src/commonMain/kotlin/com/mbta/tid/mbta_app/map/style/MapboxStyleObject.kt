package com.mbta.tid.mbta_app.map.style

import com.mbta.tid.mbta_app.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder

public interface MapboxStyleObject {
    public fun asJson(): JsonElement

    public fun toJsonString(): String = json.encodeToString(asJson())
}

internal fun JsonArrayBuilder.add(`object`: MapboxStyleObject) = add(`object`.asJson())

internal fun JsonObjectBuilder.put(key: String, value: MapboxStyleObject) = put(key, value.asJson())
