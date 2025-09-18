package com.mbta.tid.mbta_app.phoenix

internal class PredictionsForTripChannel(tripId: String) : ChannelSpec {
    override val topic = "predictions:trip:$tripId"
    override val updateEvent = "stream_data"
    override val params = emptyMap<String, Any>()
}
