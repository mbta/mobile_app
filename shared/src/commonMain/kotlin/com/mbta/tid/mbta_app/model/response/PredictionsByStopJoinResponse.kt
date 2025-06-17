package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsByStopJoinResponse(
    @SerialName("predictions_by_stop")
    internal val predictionsByStop: Map<String, Map<String, Prediction>>,
    internal val trips: Map<String, Trip>,
    internal val vehicles: Map<String, Vehicle>,
) {

    constructor(
        objects: ObjectCollectionBuilder
    ) : this(
        objects.predictions.values
            .groupBy { it.stopId }
            .mapValues { predictions -> predictions.value.associateBy { it.id } },
        objects.trips,
        objects.vehicles,
    )

    constructor(
        partialResponse: PredictionsByStopMessageResponse
    ) : this(
        mapOf(partialResponse.stopId to partialResponse.predictions),
        partialResponse.trips,
        partialResponse.vehicles,
    )

    /**
     * Merge the latest predictions for a single stop into the predictions for all stops. Removes
     * vehicles & trips that are no longer referenced in any predictions
     */
    fun mergePredictions(
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
            vehicles = updatedVehicles,
        )
    }

    /** Flattens the `predictionsByStop` field into a single map of predictions by id */
    fun toPredictionsStreamDataResponse(): PredictionsStreamDataResponse {
        val predictionsById = predictionsByStop.flatMap { it.value.values }.associateBy { it.id }
        return PredictionsStreamDataResponse(
            predictions = predictionsById,
            trips = trips,
            vehicles = vehicles,
        )
    }

    fun predictionQuantity() = predictionsByStop.map { it.value.size }.sum()

    companion object {
        val empty = PredictionsByStopJoinResponse(emptyMap(), emptyMap(), emptyMap())
    }
}

fun PredictionsByStopJoinResponse?.orEmpty() = this ?: PredictionsByStopJoinResponse.empty
