package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.AlertSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.maplibre.spatialk.geojson.Feature

public val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    serializersModule = SerializersModule {
        polymorphic(AlertSummary.Location::class) {
            defaultDeserializer { AlertSummary.Location.Unknown.serializer() }
        }
        polymorphic(AlertSummary.Timeframe::class) {
            defaultDeserializer { AlertSummary.Timeframe.Unknown.serializer() }
        }
    }
}

internal fun Feature<*, *>.propertiesToString() = json.encodeToString(properties)
