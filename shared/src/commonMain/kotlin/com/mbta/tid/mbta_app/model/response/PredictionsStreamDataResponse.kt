package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsStreamDataResponse(
    override val predictions: Map<String, Prediction>,
    override val trips: Map<String, Trip>,
    override val vehicles: Map<String, Vehicle>
) : PredictionsResponse {
    constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.predictions, objects.trips, objects.vehicles)

    override fun with(
        predictions: Map<String, Prediction>,
        trips: Map<String, Trip>,
        vehicles: Map<String, Vehicle>
    ): PredictionsResponse {
        return copy(predictions = predictions, trips = trips, vehicles = vehicles)
    }

    // In theory this should never be called, but I need to implement the function to satisfy the
    // interface
    // I thought it best for the unused implementation to "work", but it could also throw an
    // exception
    override fun mergePredictions(
        updatedPredictions: PredictionsByStopMessageResponse
    ): PredictionsResponse {
        return PredictionsStreamDataResponse(
            predictions + updatedPredictions.predictions,
            trips + updatedPredictions.trips,
            vehicles + updatedPredictions.vehicles,
        )
    }

    fun predictionQuantity() = predictions.size
}
