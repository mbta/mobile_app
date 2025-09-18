package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse

internal class VehiclesOnRouteChannel(routeIds: List<String>, directionId: Int) : ChannelSpec {
    override val topic = "vehicles:routes:${routeIds.joinToString(",")}:${directionId}"
    override val updateEvent = "stream_data"
    override val params = emptyMap<String, Any>()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): VehiclesStreamDataResponse {
            return json.decodeFromString(payload)
        }
    }
}
