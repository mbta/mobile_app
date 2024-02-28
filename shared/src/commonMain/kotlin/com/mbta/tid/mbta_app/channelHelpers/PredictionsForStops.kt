package com.mbta.tid.mbta_app.channelHelpers

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Prediction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class PredictionsForStops {
    val topic = "predictions:stops"

    fun joinPayload(stopIds: List<String>): Map<String, Any> {
        return mapOf("stop_ids" to stopIds)
    }

    @Throws(IllegalArgumentException::class)
    fun parseMessage(event: String, payload: Map<String, Any>): List<Prediction> {
        when (event) {
            "stream_data" -> {
                println("RECEIVED PAYLOAD $payload[\"predictions\"]")
                return json.decodeFromJsonElement(payload["predictions"]!! as JsonObject)
            }
            else -> throw IllegalArgumentException("Unhandled predictions channel event $event")
        }
    }
}
