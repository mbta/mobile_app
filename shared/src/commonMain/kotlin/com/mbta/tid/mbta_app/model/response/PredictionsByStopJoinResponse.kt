package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsByStopJoinResponse(
    @SerialName("predictions_by_stop") val predictionsByStop: Map<String, Map<String, Prediction>>,
    override val trips: Map<String, Trip>,
    override val vehicles: Map<String, Vehicle>
) : PredictionsResponse {

    constructor(
        objects: ObjectCollectionBuilder
    ) : this(
        objects.predictions.values
            .groupBy { it.stopId }
            .mapValues { predictions -> predictions.value.associateBy { it.id } },
        objects.trips,
        objects.vehicles
    )

    override fun with(
        predictions: Map<String, Prediction>,
        trips: Map<String, Trip>,
        vehicles: Map<String, Vehicle>
    ): PredictionsResponse {
        return copy(
            predictionsByStop =
                predictions.values
                    .groupBy { it.stopId }
                    .mapValues { it.value.associateBy { it.id } },
            trips = trips,
            vehicles = vehicles
        )
    }
    /**
     * Merge the latest predictions for a single stop into the predictions for all stops. Removes
     * vehicles & trips that are no longer referenced in any predictions
     */
    override fun mergePredictions(
        updatedPredictions: PredictionsByStopMessageResponse
    ): PredictionsByStopJoinResponse {

        val updatedPredictionsByStop: Map<String, Map<String, Prediction>> =
            predictionsByStop.plus(Pair(updatedPredictions.stopId, updatedPredictions.predictions))

        val usedTrips = mutableSetOf<String>()
        val usedVehicles = mutableSetOf<String>()
        val predictions = updatedPredictionsByStop.flatMap { it.value.values }
        predictions.forEach {
            usedTrips.add(it.tripId)
            if (it.vehicleId != null) {
                usedVehicles.add(it.vehicleId)
            }
        }

        val updatedTrips = trips.plus(updatedPredictions.trips).filterKeys { it in usedTrips }

        val updatedVehicles =
            vehicles.plus(updatedPredictions.vehicles).filterKeys { it in usedVehicles }

        return PredictionsByStopJoinResponse(
            predictionsByStop = updatedPredictionsByStop,
            trips = updatedTrips,
            vehicles = updatedVehicles
        )
    }

    override val predictions: Map<String, Prediction>
        get() {
            return predictionsByStop.flatMap { it.value.values }.associateBy { it.id }
        }

    /** Flattens the `predictionsByStop` field into a single map of predictions by id */
    fun toPredictionsStreamDataResponse(): PredictionsStreamDataResponse {
        return PredictionsStreamDataResponse(
            predictions = predictions,
            trips = trips,
            vehicles = vehicles
        )
    }

    fun predictionQuantity() = predictionsByStop.map { it.value.size }.sum()

    companion object {
        val empty = PredictionsByStopJoinResponse(emptyMap(), emptyMap(), emptyMap())
    }
}
