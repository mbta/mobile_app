package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
public data class PredictionsStreamDataResponse(
    internal val predictions: Map<String, Prediction>,
    internal val trips: Map<String, Trip>,
    internal val vehicles: Map<String, Vehicle>,
) {
    public constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.predictions, objects.trips, objects.vehicles)

    public fun predictionQuantity(): Int = predictions.size

    override fun toString(): String = "[PredictionsStreamDataResponse]"
}
