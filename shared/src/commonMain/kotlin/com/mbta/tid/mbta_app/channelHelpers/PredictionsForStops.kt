package com.mbta.tid.mbta_app.channelHelpers

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Prediction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.putJsonArray

class PredictionsForStops {
    val topic = "predictions:stops"

    fun joinPayload(stopIds: List<String>) {
        buildJsonObject { putJsonArray("stop_ids") { addAll(stopIds) } }
    }

    @Throws(IllegalArgumentException::class)
    fun parseMessage(event: String, payload: JsonObject): List<Prediction> {
        when (event) {
            "stream_data" -> {
                return json.decodeFromJsonElement(payload["predictions"]!!)
            }
            else -> throw IllegalArgumentException("Unhandled predictions channel event $event")
        }
    }
}
