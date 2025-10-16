package com.mbta.tid.mbta_app.phoenix

internal interface ChannelSpec {
    val topic: String
    val updateEvent: String
    val params: Map<String, Any>
}
