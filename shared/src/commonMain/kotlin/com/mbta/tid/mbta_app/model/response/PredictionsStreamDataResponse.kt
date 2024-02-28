package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollection
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsStreamDataResponse(
    val predictions: Map<String, Prediction>,
    val trips: Map<String, Trip>,
    val vehicles: Map<String, Vehicle>
) {
    constructor(
        objects: ObjectCollection
    ) : this(objects.predictions, objects.trips, objects.vehicles)
}
