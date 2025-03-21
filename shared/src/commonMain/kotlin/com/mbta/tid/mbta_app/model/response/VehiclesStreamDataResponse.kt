package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.PerformsPoorlyInSwift
import kotlinx.serialization.Serializable

@Serializable
data class VehiclesStreamDataResponse(@PerformsPoorlyInSwift val vehicles: Map<String, Vehicle>)
