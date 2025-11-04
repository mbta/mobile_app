package com.mbta.tid.mbta_app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.maplibre.spatialk.geojson.Feature

public val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal fun Feature<*, *>.propertiesToString() = json.encodeToString(properties)
