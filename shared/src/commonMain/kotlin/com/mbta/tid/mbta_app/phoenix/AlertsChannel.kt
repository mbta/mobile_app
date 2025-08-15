package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse

internal class AlertsChannel {
    companion object {
        val topic = "alerts:v2"

        val newDataEvent = "stream_data"

        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): AlertsStreamDataResponse {
            return json.decodeFromString(payload)
        }
    }
}
