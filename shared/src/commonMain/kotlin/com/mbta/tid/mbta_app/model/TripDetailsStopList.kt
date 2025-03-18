package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlinx.datetime.Instant

data class TripDetailsStopList
@DefaultArgumentInterop.Enabled
constructor(val tripId: String, val stops: List<Entry>, val startTerminalEntry: Entry? = null) {
    data class Entry(
        val stop: Stop,
        val stopSequence: Int,
        val disruption: RealtimePatterns.Format.Disruption?,
        val schedule: Schedule?,
        val prediction: Prediction?,
        // The prediction stop can be the same as `stop`, but it can also be a child stop which
        // contains more specific boarding information for a prediction, like the track number
        val predictionStop: Stop?,
        val vehicle: Vehicle?,
        val routes: List<Route>,
        val elevatorAlerts: List<Alert> = emptyList()
    ) {
        val trackNumber: String? =
            if (predictionStop?.shouldShowTrackNumber == true) predictionStop.platformCode else null

        fun activeElevatorAlerts(now: Instant) = elevatorAlerts.filter { it.isActive(now) }

        fun format(now: Instant, routeType: RouteType?) =
            TripInstantDisplay.from(
                prediction,
                schedule,
                vehicle,
                routeType,
                now,
                context = TripInstantDisplay.Context.TripDetails
            )
    }

    /**
     * Splits these stops around the given target stop, counting parent/child/sibling stops as
     * equivalent. If no exact match can be found by stop ID and stop sequence, matches only on stop
     * ID, picking the last copy if there are duplicates. Returns null if no match at all can be
     * found.
     */
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
            targetStopIndex =
                stops.indexOfLast { Stop.equalOrFamily(targetStopId, it.stop.id, globalData.stops) }
        }
        if (targetStopIndex == -1) {
            return null
        }

        var firstStop: Entry? = null
        var collapsedStops = stops.subList(fromIndex = 0, toIndex = targetStopIndex)
        val firstCollapsed = collapsedStops.firstOrNull()
        if (
            firstCollapsed == startTerminalEntry &&
                (firstCollapsed?.vehicle == null || firstCollapsed.vehicle.tripId != this.tripId)
        ) {
            collapsedStops = collapsedStops.drop(1)
            firstStop = firstCollapsed
        }
        val targetStop = stops[targetStopIndex]
        val followingStops =
            stops.subList(fromIndex = targetStopIndex + 1, toIndex = stops.lastIndex + 1)

        return TargetSplit(firstStop, collapsedStops, targetStop, followingStops)
    }

    data class TargetSplit(
        val firstStop: Entry? = null,
        val collapsedStops: List<Entry>,
        val targetStop: Entry,
        val followingStops: List<Entry>
    )

    private data class WorkingEntry(
        val stopId: String,
        val stopSequence: Int,
        val alert: Alert? = null,
        val schedule: Schedule? = null,
        val prediction: Prediction? = null,
        val vehicle: Vehicle? = null,
    )

    companion object {

        // TODO: Remove hardcoded IDs once the `listed_route` field is exposed by the API.
        // https://mbta.slack.com/archives/C03K6NLKKD1/p1716220182028299
        private var excludedRouteIds =
            setOf(
                "2427",
                "3233",
                "3738",
                "4050",
                "725",
                "8993",
                "116117",
                "214216",
                "441442",
                "9701",
                "9702",
                "9703",
                "Boat-F3"
            )

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
            tripId: String,
            directionId: Int,
            tripSchedules: TripSchedulesResponse?,
            tripPredictions: PredictionsStreamDataResponse?,
            vehicle: Vehicle?,
            alertsData: AlertsStreamDataResponse?,
            globalData: GlobalResponse,
        ): TripDetailsStopList? {
            if (alertsData == null) return null
            val entries = mutableMapOf<Int, WorkingEntry>()
            val routeId =
                tripPredictions?.trips?.values?.singleOrNull()?.routeId ?: tripSchedules?.routeId()
            val route = globalData.routes[routeId]

            var predictions = emptyList<Prediction>()
            if (tripPredictions != null) {
                val tripPredictionsWithCorrectRoute =
                    tripPredictions.predictions.values.filter {
                        routeId == null || it.routeId == routeId
                    }
                predictions =
                    deduplicatePredictionsByStopSequence(
                        tripPredictionsWithCorrectRoute,
                        tripSchedules,
                        globalData
                    )

                predictions.forEach { prediction -> entries.putPrediction(prediction, vehicle) }
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
                return TripDetailsStopList(tripId, emptyList())
            }

            val sortedEntries = entries.entries.sortedBy { it.key }
            val allElevatorAlerts =
                alertsData.alerts.values.filter { it.effect == Alert.Effect.ElevatorClosure }

            fun getEntry(optionalWorking: WorkingEntry?): Entry? {
                val working = optionalWorking ?: return null
                val stop = globalData.stops[working.stopId] ?: return null
                val parent = stop.resolveParent(globalData.stops)
                val parentAndChildStopIds = setOf(parent.id) + parent.childStopIds

                return Entry(
                    stop,
                    working.stopSequence,
                    getDisruption(working, alertsData, route, tripId, directionId),
                    working.schedule,
                    working.prediction,
                    globalData.stops[working.prediction?.stopId],
                    working.vehicle,
                    getTransferRoutes(working, globalData),
                    Alert.elevatorAlerts(allElevatorAlerts, parentAndChildStopIds)
                )
            }

            val startTerminalEntry = getEntry(sortedEntries.firstOrNull()?.value)
            return TripDetailsStopList(
                tripId,
                sortedEntries
                    .dropWhile {
                        if (
                            vehicle == null ||
                                vehicle.tripId != tripId ||
                                vehicle.currentStopSequence == null
                        ) {
                            false
                        } else {
                            it.value.stopSequence < vehicle.currentStopSequence ||
                                (it.value.stopSequence == vehicle.currentStopSequence &&
                                    vehicle.currentStatus == Vehicle.CurrentStatus.StoppedAt)
                        }
                    }
                    .mapNotNull { getEntry(it.value) },
                startTerminalEntry
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

        /**
         * This returns the list of routes a rider could transfer to at a stop entry on this trip.
         * It includes any routes served by typical patterns on the entry's stop's parent, children,
         * and/or sibling stops, along with any of the connecting stops.
         */
        private fun getTransferRoutes(
            entry: WorkingEntry,
            globalData: GlobalResponse
        ): List<Route> {
            val stop = globalData.stops[entry.stopId] ?: return emptyList()
            val selfOrParent =
                if (stop.parentStationId == null) stop
                else globalData.stops[stop.parentStationId] ?: return emptyList()
            // Bail if stop is not a parent but its parent stop can't be found

            val currentRoute =
                globalData.routes[entry.prediction?.routeId ?: entry.schedule?.routeId]

            val transferStopIds =
                listOf(selfOrParent.id) +
                    selfOrParent.childStopIds +
                    selfOrParent.connectingStopIds +
                    selfOrParent.connectingStopIds.flatMap {
                        globalData.stops[it]?.childStopIds ?: emptyList()
                    }

            val transferRoutes =
                transferStopIds
                    .flatMapTo(mutableSetOf()) { getFilteredRoutesForStop(it, globalData) }
                    .sortedBy(Route::sortOrder)

            return if (currentRoute != null) transferRoutes.minus(currentRoute) else transferRoutes
        }

        private fun getFilteredRoutesForStop(
            stopId: String,
            globalData: GlobalResponse
        ): Set<Route> {
            return globalData.patternIdsByStop[stopId]
                ?.map { globalData.routePatterns[it] }
                ?.mapNotNull { if (shouldExclude(it)) null else globalData.routes[it?.routeId] }
                ?.toSet()
                ?: emptySet()
        }

        /**
         * Any routes that are only found on route patterns which are not typical are excluded,
         * along with a set of hardcoded route IDs containing mostly combined bus routes which are
         * meant to be hidden on rider facing touch points.
         */
        private fun shouldExclude(pattern: RoutePattern?): Boolean {
            return pattern == null ||
                pattern.typicality != RoutePattern.Typicality.Typical ||
                excludedRouteIds.contains(pattern.routeId) ||
                pattern.routeId.startsWith("Logan-")
        }

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
                    if (!Stop.equalOrFamily(stopId, prediction.stopId, globalData.stops)) {
                        scheduleIndex--
                        continue
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

        private fun getDisruption(
            entry: WorkingEntry,
            alertsData: AlertsStreamDataResponse?,
            route: Route?,
            tripId: String,
            directionId: Int
        ): RealtimePatterns.Format.Disruption? {
            val entryTime = (entry.prediction ?: entry.schedule)?.stopTime
            val entryRouteType = route?.type

            if (entryTime == null) return null
            val alert =
                alertsData?.alerts?.values?.find { alert ->
                    alert.isActive(entryTime) &&
                        alert.anyInformedEntity {
                            it.appliesTo(
                                directionId = directionId,
                                routeId = route?.id,
                                routeType = entryRouteType,
                                stopId = entry.stopId,
                                tripId = tripId
                            )
                        } &&
                        // there's no UI yet for secondary alerts in trip details
                        alert.significance >= AlertSignificance.Major
                }
            if (alert == null) return null
            return RealtimePatterns.Format.Disruption(
                alert,
                route?.let { MapStopRoute.matching(it) }
            )
        }
    }
}
