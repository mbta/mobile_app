package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse

internal object VehiclesOnRouteChannel {
    val topic = "vehicles:route"

    val newDataEvent = "stream_data"

    fun topic(routeIds: List<String>, directionId: Int) =
        "vehicles:routes:${routeIds.joinToString(",")}:${directionId}"

    fun joinPayload(routeId: String, directionId: Int): Map<String, Any> {
        return mapOf("route_id" to routeId, "direction_id" to directionId)
    }

    @Throws(IllegalArgumentException::class)
    fun parseMessage(payload: String): VehiclesStreamDataResponse {
        return json.decodeFromString(payload)
    }
}
