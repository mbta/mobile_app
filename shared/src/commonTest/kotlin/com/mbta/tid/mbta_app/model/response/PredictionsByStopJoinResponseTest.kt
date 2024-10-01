package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.vehicle
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Vehicle
import kotlin.test.Test
import kotlin.test.assertEquals

class PredictionsByStopJoinResponseTest {
    @Test
    fun `mergePredictions replaces predictions for existing stop`() {
        val trip = trip()
        val vehicle = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val p1stop1 = prediction {
            stopId = "1"
            tripId = trip.id
            vehicleId = vehicle.id
        }

        val p1stop1updated = prediction {
            id = p1stop1.id
            stopId = "1"
            tripId = trip.id
            vehicleId = vehicle.id
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }
        val p2stop1 = prediction {
            stopId = "1"
            tripId = trip.id
            vehicleId = vehicle.id
        }

        val p1stop2 = prediction {
            stopId = "2"
            tripId = trip.id
            vehicleId = vehicle.id
        }
        val p2stop2 = prediction {
            stopId = "2"
            tripId = trip.id
            vehicleId = vehicle.id
        }

        val existingPredictions =
            PredictionsByStopJoinResponse(
                predictionsByStop =
                    mapOf(
                        "1" to mapOf(p1stop1.id to p1stop1),
                        "2" to mapOf(p1stop2.id to p1stop2, p2stop2.id to p2stop2)
                    ),
                trips = mapOf(trip.id to trip),
                vehicles = mapOf(vehicle.id to vehicle)
            )

        val stop1NewPredictions =
            PredictionsByStopMessageResponse(
                stopId = "1",
                predictions = mapOf(p1stop1updated.id to p1stop1updated, p2stop1.id to p2stop1),
                trips = mapOf(),
                vehicles = mapOf()
            )

        val result = existingPredictions.mergePredictions(stop1NewPredictions)

        assertEquals(
            mapOf(p1stop1updated.id to p1stop1updated, p2stop1.id to p2stop1),
            result.predictionsByStop["1"]
        )
        assertEquals(
            mapOf(p1stop2.id to p1stop2, p2stop2.id to p2stop2),
            result.predictionsByStop["2"]
        )
        assertEquals(mapOf(trip.id to trip), result.trips)
        assertEquals(mapOf(vehicle.id to vehicle), result.vehicles)
    }

    @Test
    fun `mergePredictions removes trips and vehicles that are no longer referenced`() {
        val trip1 = trip()
        val vehicle1 = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val trip2 = trip()
        val vehicle2 = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val p1stop1 = prediction {
            stopId = "1"
            tripId = trip1.id
            vehicleId = vehicle1.id
        }

        val p1stop2 = prediction {
            stopId = "2"
            tripId = trip2.id
            vehicleId = vehicle2.id
        }

        val existingPredictions =
            PredictionsByStopJoinResponse(
                predictionsByStop =
                    mapOf("1" to mapOf(p1stop1.id to p1stop1), "2" to mapOf(p1stop2.id to p1stop2)),
                trips = mapOf(trip1.id to trip1, trip2.id to trip2),
                vehicles = mapOf(vehicle1.id to vehicle1, vehicle2.id to vehicle2)
            )

        val stop1NewPredictions =
            PredictionsByStopMessageResponse(
                stopId = "1",
                predictions = mapOf(),
                trips = mapOf(),
                vehicles = mapOf()
            )

        val result = existingPredictions.mergePredictions(stop1NewPredictions)

        assertEquals(mapOf(), result.predictionsByStop["1"])
        assertEquals(mapOf(p1stop2.id to p1stop2), result.predictionsByStop["2"])
        assertEquals(mapOf(trip2.id to trip2), result.trips)
        assertEquals(mapOf(vehicle2.id to vehicle2), result.vehicles)
    }

    @Test
    fun `toPredictionsStreamDataResponse flattens predictionsByStop  preserves trips + vehicles`() {
        val trip1 = trip()
        val vehicle1 = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val trip2 = trip()
        val vehicle2 = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val p1stop1 = prediction {
            stopId = "1"
            tripId = trip1.id
            vehicleId = vehicle1.id
        }

        val p1stop2 = prediction {
            stopId = "2"
            tripId = trip2.id
            vehicleId = vehicle2.id
        }

        val data =
            PredictionsByStopJoinResponse(
                predictionsByStop =
                    mapOf("1" to mapOf(p1stop1.id to p1stop1), "2" to mapOf(p1stop2.id to p1stop2)),
                trips = mapOf(trip1.id to trip1, trip2.id to trip2),
                vehicles = mapOf(vehicle1.id to vehicle1, vehicle2.id to vehicle2)
            )

        val response = data.toPredictionsStreamDataResponse()

        assertEquals(mapOf(p1stop1.id to p1stop1, p1stop2.id to p1stop2), response.predictions)
        assertEquals(mapOf(trip1.id to trip1, trip2.id to trip2), response.trips)
        assertEquals(mapOf(vehicle1.id to vehicle1, vehicle2.id to vehicle2), response.vehicles)
    }

    @Test
    fun `predictionQuantity counts predictions`() {
        val data =
            PredictionsByStopJoinResponse(
                predictionsByStop =
                    mapOf(
                        "1" to mapOf("a" to prediction(), "b" to prediction()),
                        "2" to mapOf("c" to prediction())
                    ),
                trips = emptyMap(),
                vehicles = emptyMap()
            )

        assertEquals(3, data.predictionQuantity())
    }
}
