package com.mbta.tid.mbta_app.channelHelpers

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

class PredictionsForStops {
    val topic = "predictions:stops"

    val newDataEvent = "stream_data"

    fun joinPayload(stopIds: List<String>): Map<String, Any> {
        return mapOf("stop_ids" to stopIds)
    }

    @Throws(IllegalArgumentException::class)
    fun parseMessage(payload: String): PredictionsStreamDataResponse {
        return json.decodeFromString(payload)
    }
}
