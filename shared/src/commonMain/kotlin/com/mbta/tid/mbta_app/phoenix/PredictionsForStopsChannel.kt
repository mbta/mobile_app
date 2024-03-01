package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

class PredictionsForStopsChannel {
    companion object {
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
}
