package com.mbta.tid.mbta_app

import io.github.dellisd.spatialk.geojson.Feature
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun Feature.propertiesToString() = json.encodeToString(properties)
