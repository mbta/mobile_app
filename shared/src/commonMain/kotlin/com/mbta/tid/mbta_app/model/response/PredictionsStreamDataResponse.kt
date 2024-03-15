package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsStreamDataResponse(
    val predictions: Map<String, Prediction>,
    val trips: Map<String, Trip>,
    val vehicles: Map<String, Vehicle>
) : StreamDataResponse {
    constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.predictions, objects.trips, objects.vehicles)

    override val countSummary = "${predictions.size} predictions"
}
