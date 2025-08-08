package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
public data class VehiclesStreamDataResponse(val vehicles: Map<String, Vehicle>) {
    override fun toString(): String = "[VehiclesStreamDataResponse]"
}
