package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
data class VehiclesStreamDataResponse(val vehicles: Map<String, Vehicle>) {
    override fun toString() = "[VehiclesStreamDataResponse]"
}
