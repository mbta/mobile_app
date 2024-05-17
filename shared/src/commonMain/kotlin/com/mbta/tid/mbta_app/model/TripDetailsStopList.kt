package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant

data class TripDetailsStopList(val stops: List<Entry>) {
    data class Entry(
        val stop: Stop,
        val stopSequence: Int,
        val schedule: Schedule?,
        val prediction: Prediction?,
        val vehicle: Vehicle?,
    ) {
        // we want very slightly different logic than the UpcomingTrip itself has
        // specifically, we want to still render predictions that are arrival-only
        fun format(now: Instant): UpcomingTrip.Format {
            prediction?.status?.let {
                return UpcomingTrip.Format.Overridden(it)
            }
            if (prediction == null) {
                val scheduleTime = schedule?.scheduleTime
                return if (scheduleTime == null) {
                    UpcomingTrip.Format.Hidden
                } else {
                    UpcomingTrip.Format.Schedule(scheduleTime)
                }
            }
            if (prediction.predictionTime == null) {
                return UpcomingTrip.Format.Hidden
            }
            val timeRemaining = prediction.predictionTime.minus(now)
            if (
                vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                    vehicle.stopId == prediction.stopId &&
                    vehicle.tripId == prediction.tripId &&
                    timeRemaining <= BOARDING_CUTOFF
            ) {
                return UpcomingTrip.Format.Boarding
            }
            if (timeRemaining <= ARRIVAL_CUTOFF) {
                return UpcomingTrip.Format.Arriving
            }
            if (timeRemaining <= APPROACH_CUTOFF) {
                return UpcomingTrip.Format.Approaching
            }
            if (timeRemaining > DISTANT_FUTURE_CUTOFF) {
                return UpcomingTrip.Format.DistantFuture(prediction.predictionTime)
            }
            val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
            return UpcomingTrip.Format.Minutes(minutes)
        }
    }

    fun splitForTarget(
        targetStopId: String,
        targetStopSequence: Int,
        globalData: GlobalResponse
    ): TargetSplit? {
        var targetStopIndex =
            stops.indexOfFirst {
                Stop.equalOrFamily(targetStopId, it.stop.id, globalData.stops) &&
                    it.stopSequence == targetStopSequence
            }
        if (targetStopIndex == -1) {
            // couldn't match by stop sequence, so just match by stop id
            targetStopIndex =
                stops.indexOfLast { Stop.equalOrFamily(targetStopId, it.stop.id, globalData.stops) }
        }
        if (targetStopIndex == -1) {
            return null
        }

        val collapsedStops = stops.subList(fromIndex = 0, toIndex = targetStopIndex)
        val targetStop = stops[targetStopIndex]
        val followingStops =
            stops.subList(fromIndex = targetStopIndex + 1, toIndex = stops.lastIndex + 1)

        return TargetSplit(collapsedStops, targetStop, followingStops)
    }

    data class TargetSplit(
        val collapsedStops: List<Entry>,
        val targetStop: Entry,
        val followingStops: List<Entry>
    )

    private data class WorkingEntry(
        val stopId: String,
        val stopSequence: Int,
        val schedule: Schedule? = null,
        val prediction: Prediction? = null,
        val vehicle: Vehicle? = null,
    )

    companion object {
        private fun MutableMap<Int, WorkingEntry>.putSchedule(schedule: Schedule) {
            put(
                schedule.stopSequence,
                get(schedule.stopSequence)?.copy(schedule = schedule)
                    ?: WorkingEntry(schedule.stopId, schedule.stopSequence, schedule = schedule)
            )
        }

        private fun MutableMap<Int, WorkingEntry>.putPrediction(
            prediction: Prediction,
            vehicle: Vehicle?
        ) {
            put(
                prediction.stopSequence,
                get(prediction.stopSequence)?.copy(prediction = prediction, vehicle = vehicle)
                    ?: WorkingEntry(
                        prediction.stopId,
                        prediction.stopSequence,
                        prediction = prediction,
                        vehicle = vehicle
                    )
            )
        }

        private fun MutableMap<Int, WorkingEntry>.putEmpty(stopId: String, stopSequence: Int) {
            if (!containsKey(stopSequence)) {
                put(stopSequence, WorkingEntry(stopId, stopSequence))
            }
        }

        fun fromPieces(
            tripSchedules: TripSchedulesResponse?,
            tripPredictions: PredictionsStreamDataResponse?,
            globalData: GlobalResponse,
        ): TripDetailsStopList? {
            val entries = mutableMapOf<Int, WorkingEntry>()

            val predictions =
                deduplicatePredictionsByStopSequence(
                    tripPredictions?.predictions?.values.orEmpty(),
                    tripSchedules,
                    globalData
                )

            predictions.forEach { prediction ->
                entries.putPrediction(
                    prediction,
                    tripPredictions?.vehicles?.get(prediction.vehicleId)
                )
            }

            if (tripSchedules is TripSchedulesResponse.Schedules) {
                tripSchedules.schedules.forEach { entries.putSchedule(it) }
            } else if (tripSchedules is TripSchedulesResponse.StopIds) {
                val aligner =
                    ScheduleStopSequenceAligner(
                        tripSchedules.stopIds,
                        predictions,
                        globalData,
                        entries
                    )
                aligner.run()
            }

            if (entries.isEmpty()) {
                return null
            }
            return TripDetailsStopList(
                entries.entries
                    .sortedBy { it.key }
                    .map {
                        Entry(
                            globalData.stops.getValue(it.value.stopId),
                            it.value.stopSequence,
                            it.value.schedule,
                            it.value.prediction,
                            it.value.vehicle,
                        )
                    }
            )
        }

        // unfortunately, stop sequence is not always actually unique
        // conveniently, it seems like duplicates are rare
        private fun deduplicatePredictionsByStopSequence(
            predictions: Collection<Prediction>,
            tripSchedules: TripSchedulesResponse?,
            globalData: GlobalResponse
        ) =
            predictions
                .groupBy { it.stopSequence }
                .mapValues { (stopSequence, predictions) ->
                    if (predictions.size == 1) {
                        predictions.single()
                    } else {
                        // the only encountered case so far here has the incorrect duplicate not
                        // included in the schedule
                        val scheduledStopIds =
                            tripSchedules?.stops(globalData).orEmpty().map { it.id }.toSet()
                        val scheduledPredictions =
                            predictions.filter { it.stopId in scheduledStopIds }
                        check(scheduledPredictions.size == 1) {
                            "Trip ${predictions.first().tripId} has duplicate predictions $predictions at stop sequence $stopSequence"
                        }
                        scheduledPredictions.single()
                    }
                }
                .values
                .sortedBy { it.stopSequence }

        private class ScheduleStopSequenceAligner(
            val stopIds: List<String>,
            val predictions: List<Prediction>,
            val globalData: GlobalResponse,
            val entries: MutableMap<Int, WorkingEntry>
        ) {
            var lastStopSequence: Int? = null
            var lastDelta: Int? = null
            var scheduleIndex = stopIds.lastIndex
            var predictionIndex = predictions.lastIndex
            val lastPrediction = predictions.lastOrNull()
            var unpredictedTrailingSchedules = 0

            fun run() {
                countUnpredictedTrailingSchedules()
                zipPredictedSchedules()
                // everything needs a stop sequence, whether it matches to a prediction or not
                assignDefaultStateIfMissing()
                inferUnpredictedLeadingSchedules()
                inferUnpredictedTrailingSchedules()
            }

            fun countUnpredictedTrailingSchedules() {
                if (lastPrediction != null) {
                    while (
                        scheduleIndex in stopIds.indices &&
                            !Stop.equalOrFamily(
                                stopIds[scheduleIndex],
                                lastPrediction.stopId,
                                globalData.stops
                            )
                    ) {
                        scheduleIndex--
                        unpredictedTrailingSchedules++
                    }
                }
            }

            fun zipPredictedSchedules() {
                while (scheduleIndex in stopIds.indices && predictionIndex in predictions.indices) {
                    val stopId = stopIds[scheduleIndex]
                    val prediction = predictions[predictionIndex]
                    check(Stop.equalOrFamily(stopId, prediction.stopId, globalData.stops)) {
                        "predictions=$predictions stopIds=$stopIds predictionIndex=$predictionIndex scheduleIndex=$scheduleIndex"
                    }
                    lastDelta = lastStopSequence?.minus(prediction.stopSequence)
                    lastStopSequence = prediction.stopSequence

                    scheduleIndex--
                    predictionIndex--
                }
            }

            fun assignDefaultStateIfMissing() {
                lastStopSequence = lastStopSequence ?: 1000
                lastDelta = lastDelta ?: 1
            }

            fun inferUnpredictedLeadingSchedules() {
                while (scheduleIndex in stopIds.indices) {
                    val stopId = stopIds[scheduleIndex]
                    lastStopSequence = lastStopSequence!! - lastDelta!!
                    entries.putEmpty(stopId, lastStopSequence!!)
                    scheduleIndex--
                }
            }

            fun inferUnpredictedTrailingSchedules() {
                if (lastPrediction != null && unpredictedTrailingSchedules > 0) {
                    scheduleIndex = stopIds.lastIndex - unpredictedTrailingSchedules + 1
                    lastStopSequence = lastPrediction.stopSequence
                    while (scheduleIndex in stopIds.indices) {
                        val stopId = stopIds[scheduleIndex]
                        lastStopSequence = lastStopSequence!! + lastDelta!!
                        entries.putEmpty(stopId, lastStopSequence!!)
                        scheduleIndex++
                    }
                }
            }
        }
    }
}
