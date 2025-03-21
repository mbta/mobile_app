package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.PerformsPoorlyInSwift
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsStreamDataResponse(
    @PerformsPoorlyInSwift val predictions: Map<String, Prediction>,
    @PerformsPoorlyInSwift val trips: Map<String, Trip>,
    @PerformsPoorlyInSwift val vehicles: Map<String, Vehicle>
) {
    constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.predictions, objects.trips, objects.vehicles)

    fun predictionQuantity() = predictions.size
}
