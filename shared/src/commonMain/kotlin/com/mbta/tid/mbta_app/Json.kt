package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.response.PushNotificationPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.maplibre.spatialk.geojson.Feature

public val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    serializersModule = SerializersModule {
        polymorphic(AlertSummary.Piece::class) {
            defaultDeserializer { AlertSummary.Unknown.serializer() }
        }
        polymorphic(PushNotificationPayload.Title::class) {
            defaultDeserializer { PushNotificationPayload.Title.Unknown.serializer() }
        }
    }
}

internal fun Feature<*, *>.propertiesToString() = json.encodeToString(properties)
