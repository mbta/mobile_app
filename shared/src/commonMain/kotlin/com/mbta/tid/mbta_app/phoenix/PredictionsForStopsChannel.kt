package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

class PredictionsForStopsChannel(stopIds: List<String>) :
    ChannelSpec<PredictionsStreamDataResponse>() {
    override val topic = "predictions:stops"

    override val newDataEvent = "stream_data"

    override val joinPayload = mapOf("stop_ids" to stopIds)

    @Throws(IllegalArgumentException::class)
    override fun parseMessage(payload: String): PredictionsStreamDataResponse {
        return json.decodeFromString(payload)
    }
}
