package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

internal class PredictionsForStopsChannel {
    companion object {
        val topic = "predictions:stops"

        val newDataEvent = "stream_data"

        fun topicV2(stopIds: List<String>) = "predictions:stops:v2:${stopIds.joinToString(",")}"

        fun joinPayload(stopIds: List<String>): Map<String, Any> {
            return mapOf("stop_ids" to stopIds)
        }

        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): PredictionsStreamDataResponse {
            return json.decodeFromString(payload)
        }

        @Throws(IllegalArgumentException::class)
        fun parseV2JoinMessage(payload: String): PredictionsByStopJoinResponse {
            return json.decodeFromString(payload)
        }

        @Throws(IllegalArgumentException::class)
        fun parseV2Message(payload: String): PredictionsByStopMessageResponse {
            return json.decodeFromString(payload)
        }
    }
}
