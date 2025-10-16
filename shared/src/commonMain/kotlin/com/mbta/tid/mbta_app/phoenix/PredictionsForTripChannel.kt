package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse

internal class PredictionsForTripChannel(tripId: String) : ChannelSpec {
    override val topic = "predictions:trip:$tripId"
    override val updateEvent = "stream_data"
    override val params = emptyMap<String, Any>()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): PredictionsStreamDataResponse {
            return json.decodeFromString(payload)
        }
    }
}
