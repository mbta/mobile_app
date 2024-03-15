package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.response.StreamDataResponse

abstract class ChannelSpec<Data : StreamDataResponse> {
    abstract val topic: String
    abstract val newDataEvent: String
    open val joinPayload: Map<String, Any> = emptyMap()

    @Throws(IllegalArgumentException::class) abstract fun parseMessage(payload: String): Data
}
