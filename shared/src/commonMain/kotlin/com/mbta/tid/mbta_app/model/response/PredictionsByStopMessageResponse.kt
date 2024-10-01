package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsByStopMessageResponse(
    @SerialName("stop_id") val stopId: String,
    val predictions: Map<String, Prediction>,
    val trips: Map<String, Trip>,
    val vehicles: Map<String, Vehicle>
) {}
