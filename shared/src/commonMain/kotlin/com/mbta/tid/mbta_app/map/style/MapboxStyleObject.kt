package com.mbta.tid.mbta_app.map.style

import com.mbta.tid.mbta_app.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder

interface MapboxStyleObject {
    fun asJson(): JsonElement

    fun toJsonString() = json.encodeToString(asJson())
}

fun JsonArrayBuilder.add(`object`: MapboxStyleObject) = add(`object`.asJson())

fun JsonObjectBuilder.put(key: String, value: MapboxStyleObject) = put(key, value.asJson())
