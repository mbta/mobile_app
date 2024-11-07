package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle

interface PredictionsResponse {
    val predictions: Map<String, Prediction>
    val trips: Map<String, Trip>
    val vehicles: Map<String, Vehicle>

    fun with(
        predictions: Map<String, Prediction> = this.predictions,
        trips: Map<String, Trip> = this.trips,
        vehicles: Map<String, Vehicle> = this.vehicles
    ): PredictionsResponse

    fun mergePredictions(updatedPredictions: PredictionsByStopMessageResponse): PredictionsResponse
}
