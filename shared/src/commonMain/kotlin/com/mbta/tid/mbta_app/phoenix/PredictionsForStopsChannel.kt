package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

internal object PredictionsForStopsChannel {
    class V1(stopIds: List<String>) : ChannelSpec {
        override val topic = "predictions:stops"
        override val updateEvent = "stream_data"
        override val params = mapOf("stop_ids" to stopIds)
    }

    class V2(stopIds: List<String>) : ChannelSpec {
        override val topic = "predictions:stops:v2:${stopIds.joinToString(",")}"
        override val updateEvent = "stream_data"
        override val params = emptyMap<String, Any>()
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
