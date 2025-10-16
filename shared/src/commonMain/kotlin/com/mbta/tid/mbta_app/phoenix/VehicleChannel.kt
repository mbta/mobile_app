package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse

internal class VehicleChannel(vehicleId: String) : ChannelSpec {
    override val topic = "vehicle:id:${vehicleId}"
    override val updateEvent = "stream_data"
    override val params = emptyMap<String, Any>()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): VehicleStreamDataResponse {
            return json.decodeFromString(payload)
        }
    }
}
