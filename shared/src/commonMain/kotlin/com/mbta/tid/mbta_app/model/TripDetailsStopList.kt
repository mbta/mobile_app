package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public data class TripDetailsStopList
@DefaultArgumentInterop.Enabled
constructor(val trip: Trip, val stops: List<Entry>, val startTerminalEntry: Entry? = null) {

    public data class Entry
    @DefaultArgumentInterop.Enabled
    constructor(
        val stop: Stop,
        val stopSequence: Int,
        val disruption: UpcomingFormat.Disruption?,
        val schedule: Schedule?,
        val prediction: Prediction?,
        // The prediction stop can be the same as `stop`, but it can also be a child stop which
        // contains more specific boarding information for a prediction, like the track number
        val predictionStop: Stop? = stop.takeIf { prediction != null },
        val vehicle: Vehicle?,
        val routes: List<Route>,
        val elevatorAlerts: List<Alert> = emptyList(),
    ) {
        val trackNumber: String? =
            if (predictionStop?.shouldShowTrackNumber == true) predictionStop.platformCode else null

        internal val isTruncating = disruption?.alert?.hasNoThroughService == true

        public fun activeElevatorAlerts(now: EasternTimeInstant): List<Alert> =
            elevatorAlerts.filter { it.isActive(now) }

        /**
         * Gets the time to display for this entry, or an alert to be displayed instead.
         *
         * @return [disruption], an [UpcomingFormat.Some] with a single entry, or null
         */
        public fun format(trip: Trip, now: EasternTimeInstant, route: Route): UpcomingFormat? {
            if (disruption != null) {
                // ignore activities on platforms since they may be wrong or they may be correct in
                // a way that doesn’t match how service is being run
                if (isTruncating) {
                    // if the alert represents a truncation of service, either this is the first
                    // stop of the alert and we want to show its arrival time or this is a later
                    // stop in the alert and it was discarded in splitForTarget so this was never
                    // called
                } else {
                    // if the alert does not represent a truncation of service (e.g. stop closure),
                    // we do want to replace the time with the alert
                    return disruption
                }
            }

            return UpcomingFormat.Some(
                UpcomingTrip(trip, schedule, prediction, predictionStop, vehicle)
                    .format(now, route, TripInstantDisplay.Context.TripDetails) ?: return null,
                secondaryAlert = null,
            )
        }
    }

    /**
     * Splits these stops around the given target stop, counting parent/child/sibling stops as
     * equivalent. If no exact match can be found by stop ID and stop sequence, matches only on stop
     * ID, picking the last copy if there are duplicates. Returns all entries in
     * [TargetSplit.followingStops] if no match at all can be found.
     */
    public fun splitForTarget(
        targetStopId: String,
        targetStopSequence: Int?,
        globalData: GlobalResponse?,
    ): TargetSplit {
        val targetStopIndex =
            stops
                .indexOfFirst {
                    Stop.equalOrFamily(targetStopId, it.stop.id, globalData?.stops.orEmpty()) &&
                        it.stopSequence == targetStopSequence
                }
                .takeUnless { it == -1 }
                ?: stops
                    .indexOfLast {
                        Stop.equalOrFamily(targetStopId, it.stop.id, globalData?.stops.orEmpty())
                    }
                    .takeUnless { it == -1 }

        var firstStop: Entry? = null
        var collapsedStops =
            if (targetStopIndex != null) stops.subList(fromIndex = 0, toIndex = targetStopIndex)
            else null
        val firstCollapsed = collapsedStops?.firstOrNull()
        if (
            firstCollapsed == startTerminalEntry &&
                (firstCollapsed?.vehicle == null || firstCollapsed.vehicle.tripId != this.trip.id)
        ) {
            collapsedStops = collapsedStops?.drop(1)
            firstStop = firstCollapsed
        }
        val targetStop = targetStopIndex?.let { stops[it] }
        val followingStops =
            if (targetStopIndex != null)
                stops.subList(fromIndex = targetStopIndex + 1, toIndex = stops.lastIndex + 1)
            else stops

        val truncatedStopIndex =
            followingStops
                .indexOfFirst { it.isTruncating }
                .takeUnless { it == -1 || it == followingStops.lastIndex }
        val isTruncated = truncatedStopIndex != null
        val truncatedFollowingStops =
            if (truncatedStopIndex != null)
                followingStops.subList(fromIndex = 0, toIndex = truncatedStopIndex + 1)
            else followingStops

        return TargetSplit(
            firstStop,
            collapsedStops,
            targetStop,
            truncatedFollowingStops,
            isTruncated,
        )
    }

    public data class TargetSplit
    internal constructor(
        val firstStop: Entry? = null,
        val collapsedStops: List<Entry>?,
        val targetStop: Entry?,
        val followingStops: List<Entry>,
        val isTruncatedByLastAlert: Boolean = false,
    )

    private data class WorkingEntry(
        val stopId: String,
        val stopSequence: Int,
        val alert: Alert? = null,
        val schedule: Schedule? = null,
        val prediction: Prediction? = null,
        val vehicle: Vehicle? = null,
    )

    override fun toString(): String = "[TripDetailsStopList]"

    public companion object {
        private fun MutableMap<Int, WorkingEntry>.putSchedule(schedule: Schedule) {
            put(
                schedule.stopSequence,
                get(schedule.stopSequence)?.copy(schedule = schedule)
                    ?: WorkingEntry(schedule.stopId, schedule.stopSequence, schedule = schedule),
            )
        }

        private fun MutableMap<Int, WorkingEntry>.putPrediction(
            prediction: Prediction,
            vehicle: Vehicle?,
        ) {
            put(
                prediction.stopSequence,
                get(prediction.stopSequence)?.copy(prediction = prediction, vehicle = vehicle)
                    ?: WorkingEntry(
                        prediction.stopId,
                        prediction.stopSequence,
                        prediction = prediction,
                        vehicle = vehicle,
                    ),
            )
        }

        private fun MutableMap<Int, WorkingEntry>.putEmpty(stopId: String, stopSequence: Int) {
            if (!containsKey(stopSequence)) {
                put(stopSequence, WorkingEntry(stopId, stopSequence))
            }
        }

        public suspend fun fromPieces(
            trip: Trip,
            tripSchedules: TripSchedulesResponse?,
            tripPredictions: PredictionsStreamDataResponse?,
            vehicle: Vehicle?,
            alertsData: AlertsStreamDataResponse?,
            globalData: GlobalResponse,
        ): TripDetailsStopList? =
            withContext(Dispatchers.Default) {
                if (alertsData == null) return@withContext null
                val entries = mutableMapOf<Int, WorkingEntry>()
                val routeId =
                    tripPredictions?.trips?.values?.singleOrNull()?.routeId
                        ?: tripSchedules?.routeId()
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
                            globalData,
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
                            entries,
                        )
                    aligner.run()
                }

                if (entries.isEmpty()) {
                    return@withContext TripDetailsStopList(trip, emptyList())
                }

                val sortedEntries = entries.entries.sortedBy { it.key }
                val fallbackTime =
                    sortedEntries
                        .mapNotNull { it.value.prediction?.stopTime ?: it.value.schedule?.stopTime }
                        .lastOrNull()
                val allElevatorAlerts =
                    alertsData.alerts.values.filter { it.effect == Alert.Effect.ElevatorClosure }

                fun getEntry(optionalWorking: WorkingEntry?): Entry? {
                    val working = optionalWorking ?: return null
                    val stop = globalData.stops[working.stopId] ?: return null
                    val parent = stop.resolveParent(globalData.stops)
                    val parentAndChildStopIds = setOf(parent.id) + parent.childStopIds
                    val disruption =
                        getDisruption(
                            working,
                            fallbackTime,
                            alertsData,
                            route,
                            trip.id,
                            trip.directionId,
                        )
                    return Entry(
                        stop,
                        working.stopSequence,
                        disruption,
                        working.schedule,
                        working.prediction,
                        globalData.stops[working.prediction?.stopId],
                        working.vehicle,
                        getTransferRoutes(working, globalData),
                        Alert.elevatorAlerts(allElevatorAlerts, parentAndChildStopIds),
                    )
                }

                val startTerminalEntry = getEntry(sortedEntries.firstOrNull()?.value)
                TripDetailsStopList(
                    trip,
                    sortedEntries
                        .dropWhile {
                            if (
                                vehicle == null ||
                                    vehicle.tripId != trip.id ||
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
                    startTerminalEntry,
                )
            }

        // unfortunately, stop sequence is not always actually unique
        // conveniently, it seems like duplicates are rare
        private fun deduplicatePredictionsByStopSequence(
            predictions: Collection<Prediction>,
            tripSchedules: TripSchedulesResponse?,
            globalData: GlobalResponse,
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
            globalData: GlobalResponse,
        ): List<Route> {
            return getTransferRoutes(
                entry.stopId,
                entry.prediction?.routeId ?: entry.schedule?.routeId,
                globalData,
            )
        }

        internal fun getTransferRoutes(
            stopId: String,
            currentRouteId: Route.Id?,
            globalData: GlobalResponse,
        ): List<Route> {
            val stop = globalData.stops[stopId] ?: return emptyList()
            val selfOrParent =
                if (stop.parentStationId == null) stop
                else globalData.stops[stop.parentStationId] ?: return emptyList()
            // Bail if stop is not a parent but its parent stop can't be found

            val currentRoute = globalData.routes[currentRouteId]

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
            globalData: GlobalResponse,
        ): Set<Route> {
            return globalData.patternIdsByStop[stopId]
                .orEmpty()
                .mapNotNull { routePatternId ->
                    val routePattern = globalData.routePatterns[routePatternId]
                    val route = globalData.getRoute(routePattern?.routeId)
                    if (route != null && routePattern != null && shouldInclude(route, routePattern))
                        route
                    else null
                }
                .toSet()
        }

        /** Only listed routes which are visited by a typical route pattern are included. */
        private fun shouldInclude(route: Route, pattern: RoutePattern): Boolean {
            return pattern.typicality == RoutePattern.Typicality.Typical && route.isListedRoute
        }

        private class ScheduleStopSequenceAligner(
            val stopIds: List<String>,
            val predictions: List<Prediction>,
            val globalData: GlobalResponse,
            val entries: MutableMap<Int, WorkingEntry>,
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
                                globalData.stops,
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
            fallbackTime: EasternTimeInstant?,
            alertsData: AlertsStreamDataResponse?,
            route: Route?,
            tripId: String,
            directionId: Int,
        ): UpcomingFormat.Disruption? {
            val entryTime = (entry.prediction ?: entry.schedule)?.stopTime ?: fallbackTime
            val entryRouteType = route?.type

            val alert =
                alertsData?.alerts?.values?.find { alert ->
                    (entryTime?.let { alert.isActive(it) } ?: true) &&
                        alert.anyInformedEntity {
                            it.appliesTo(
                                directionId = directionId,
                                routeId = route?.id,
                                routeType = entryRouteType,
                                stopId = entry.stopId,
                                tripId = tripId,
                            )
                        } &&
                        // there's no UI yet for secondary alerts in trip details
                        alert.significance >= AlertSignificance.Major
                }
            if (alert == null) return null
            return UpcomingFormat.Disruption(alert, route?.let { MapStopRoute.matching(it) })
        }
    }
}
