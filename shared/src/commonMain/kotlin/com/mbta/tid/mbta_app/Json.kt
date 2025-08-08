package com.mbta.tid.mbta_app

import io.github.dellisd.spatialk.geojson.Feature
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal fun Feature.propertiesToString() = json.encodeToString(properties)
