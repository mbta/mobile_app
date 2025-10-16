package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse

internal object AlertsChannel : ChannelSpec {
    override val topic = "alerts:v2"

    override val updateEvent = "stream_data"

    override val params = emptyMap<String, Any>()

    @Throws(IllegalArgumentException::class)
    fun parseMessage(payload: String): AlertsStreamDataResponse {
        return json.decodeFromString(payload)
    }
}
