package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse

object AlertsChannel : ChannelSpec<AlertsStreamDataResponse>() {
    override val topic = "alerts"

    override val newDataEvent = "stream_data"

    @Throws(IllegalArgumentException::class)
    override fun parseMessage(payload: String): AlertsStreamDataResponse {
        return json.decodeFromString(payload)
    }
}
