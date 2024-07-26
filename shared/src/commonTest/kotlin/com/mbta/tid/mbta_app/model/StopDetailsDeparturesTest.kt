package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class StopDetailsDeparturesTest {
    @Test
    fun `StopDetailsDepartures finds trips`() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        val routePattern1 = objects.routePattern(route) { representativeTrip { headsign = "A" } }
        val routePattern2 =
            objects.routePattern(route) {
                representativeTrip { headsign = "B" }
                typicality = RoutePattern.Typicality.Typical
            }
        val stop = objects.stop()

        val time1 = Instant.parse("2024-04-02T16:29:22Z")

        val trip1 = objects.trip(routePattern1)
        val schedule1 =
            objects.schedule {
                this.trip = trip1
                stopId = stop.id
                departureTime = time1
                stopSequence = 4
            }
        val prediction1 = objects.prediction(schedule1) { departureTime = time1 }

        val time2 = Instant.parse("2024-04-02T17:11:31Z")
        val trip2 = objects.trip(routePattern1)
        val schedule2 =
            objects.schedule {
                this.trip = trip2
                stopId = stop.id
                departureTime = time2
                stopSequence = 4
            }

        assertEquals(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                route,
                                "A",
                                null,
                                listOf(routePattern1),
                                listOf(
                                    objects.upcomingTrip(schedule1, prediction1),
                                    objects.upcomingTrip(schedule2)
                                )
                            ),
                            RealtimePatterns.ByHeadsign(
                                route,
                                "B",
                                null,
                                listOf(routePattern2),
                                listOf()
                            )
                        )
                    )
                )
            ),
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                null,
                setOf(),
                filterAtTime = time1,
            )
        )
    }

    @Test
    fun `StopDetailsDepartures finds trips for line`() {
        val objects = ObjectCollectionBuilder()

        val stop = objects.stop()
        val line = objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternB1 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternB2 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripB1 = objects.trip(routePatternB1)
        val tripB2 = objects.trip(routePatternB2)

        val routeC =
            objects.route {
                id = "C"
                sortOrder = 2
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternC1 =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternC2 =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripC1 = objects.trip(routePatternC1)
        val tripC2 = objects.trip(routePatternC2)

        val routeE =
            objects.route {
                id = "E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Park St & North")
            }
        val routePatternE1 =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Heath Street" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternE2 =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Medford/Tufts" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripE1 = objects.trip(routePatternE1)
        val tripE2 = objects.trip(routePatternE2)

        val time = Instant.parse("2024-03-18T10:41:13-04:00")

        val schedB1 =
            objects.schedule {
                trip = tripB1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val schedB2 =
            objects.schedule {
                trip = tripB2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 4.minutes
            }
        val schedC1 =
            objects.schedule {
                trip = tripC1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }
        val schedC2 =
            objects.schedule {
                trip = tripC2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 5.minutes
            }
        val schedE1 =
            objects.schedule {
                trip = tripE1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }
        val schedE2 =
            objects.schedule {
                trip = tripE2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 6.minutes
            }

        val predB1 = objects.prediction(schedB1) { departureTime = time + 1.5.minutes }
        val predB2 = objects.prediction(schedB2) { departureTime = time + 4.5.minutes }
        val predC1 = objects.prediction(schedC1) { departureTime = time + 2.3.minutes }
        val predC2 = objects.prediction(schedC2) { departureTime = time + 5.3.minutes }
        val predE1 = objects.prediction(schedE1) { departureTime = time + 2.3.minutes }
        val predE2 = objects.prediction(schedE2) { departureTime = time + 6.3.minutes }

        val directionWest = Direction("West", "Kenmore & West", 0)
        val directionEast = Direction("East", "Park St & North", 1)

        assertEquals(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        routes = listOf(routeB, routeC, routeE),
                        line = line,
                        stop,
                        listOf(
                            RealtimePatterns.ByDirection(
                                line,
                                listOf(routeB, routeC),
                                directionWest,
                                listOf(routePatternB1, routePatternC1),
                                listOf(
                                    objects.upcomingTrip(schedB1, predB1),
                                    objects.upcomingTrip(schedC1, predC1),
                                )
                            ),
                            RealtimePatterns.ByHeadsign(
                                routeE,
                                "Heath Street",
                                line,
                                listOf(routePatternE1),
                                listOf(objects.upcomingTrip(schedE1, predE1))
                            ),
                            RealtimePatterns.ByDirection(
                                line,
                                listOf(routeB, routeC, routeE),
                                directionEast,
                                listOf(routePatternB2, routePatternC2, routePatternE2),
                                listOf(
                                    objects.upcomingTrip(schedB2, predB2),
                                    objects.upcomingTrip(schedC2, predC2),
                                    objects.upcomingTrip(schedE2, predE2),
                                )
                            ),
                        ),
                        listOf(Direction("West", null, 0), directionEast)
                    )
                )
            ),
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(
                        stop.id to
                            listOf(
                                routePatternB1.id,
                                routePatternB2.id,
                                routePatternC1.id,
                                routePatternC2.id,
                                routePatternE1.id,
                                routePatternE2.id
                            )
                    )
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                null,
                setOf(),
                filterAtTime = time,
            )
        )
    }

    @Test
    fun `StopDetailsDepartures shows partial data and filters after loading`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop()

        val time = Clock.System.now()

        data class PatternInfo(
            val headsign: String,
            val routePattern: RoutePattern,
            val scheduledTrip: UpcomingTrip?,
            val predictedTrip: UpcomingTrip?,
        ) {
            fun patternsByHeadsign(trips: List<UpcomingTrip>?) =
                RealtimePatterns.ByHeadsign(route, headsign, null, listOf(routePattern), trips)
        }

        fun buildPattern(scheduled: Boolean, predicted: Boolean): PatternInfo {
            val headsign = buildString {
                if (scheduled) {
                    append("Scheduled")
                } else {
                    append("Unscheduled")
                }
                append(" & ")
                if (predicted) {
                    append("Predicted")
                } else {
                    append("Unpredicted")
                }
            }
            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Atypical
                    representativeTrip { this.headsign = headsign }
                }
            val scheduledTrip =
                if (scheduled) {
                    val scheduledTrip = objects.trip(routePattern)
                    val schedule =
                        objects.schedule {
                            trip = scheduledTrip
                            stopId = stop.id
                            departureTime = time
                        }
                    objects.upcomingTrip(schedule)
                } else null
            val predictedTrip =
                if (predicted) {
                    val predictedTrip = objects.trip(routePattern)
                    val prediction =
                        objects.prediction {
                            trip = predictedTrip
                            stopId = stop.id
                            departureTime = time
                        }
                    objects.upcomingTrip(prediction)
                } else null

            return PatternInfo(headsign, routePattern, scheduledTrip, predictedTrip)
        }

        val scheduledPredicted = buildPattern(scheduled = true, predicted = true)
        val scheduledUnpredicted = buildPattern(scheduled = true, predicted = false)
        val unscheduledPredicted = buildPattern(scheduled = false, predicted = true)
        val unscheduledUnpredicted = buildPattern(scheduled = false, predicted = false)

        fun expected(vararg pattern: RealtimePatterns.ByHeadsign): StopDetailsDepartures =
            StopDetailsDepartures(listOf(PatternsByStop(route, stop, pattern.toList())))

        fun actual(includeSchedules: Boolean = true, includePredictions: Boolean = true) =
            StopDetailsDepartures(
                stop,
                GlobalResponse(objects, mapOf(stop.id to objects.routePatterns.keys.toList())),
                ScheduleResponse(objects).takeIf { includeSchedules },
                PredictionsStreamDataResponse(objects).takeIf { includePredictions },
                null,
                setOf(),
                filterAtTime = time
            )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(null),
                scheduledUnpredicted.patternsByHeadsign(null),
                unscheduledPredicted.patternsByHeadsign(null),
                unscheduledUnpredicted.patternsByHeadsign(null)
            ),
            actual(includeSchedules = false, includePredictions = false)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(listOf(scheduledPredicted.predictedTrip!!)),
                scheduledUnpredicted.patternsByHeadsign(emptyList()),
                unscheduledPredicted.patternsByHeadsign(
                    listOf(unscheduledPredicted.predictedTrip!!)
                ),
                unscheduledUnpredicted.patternsByHeadsign(emptyList())
            ),
            actual(includeSchedules = false, includePredictions = true)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(listOf(scheduledPredicted.scheduledTrip!!)),
                scheduledUnpredicted.patternsByHeadsign(
                    listOf(scheduledUnpredicted.scheduledTrip!!)
                ),
                unscheduledPredicted.patternsByHeadsign(emptyList()),
                unscheduledUnpredicted.patternsByHeadsign(emptyList())
            ),
            actual(includeSchedules = true, includePredictions = false)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(
                    listOf(scheduledPredicted.scheduledTrip, scheduledPredicted.predictedTrip)
                ),
                scheduledUnpredicted.patternsByHeadsign(listOf(scheduledUnpredicted.scheduledTrip)),
                unscheduledPredicted.patternsByHeadsign(listOf(unscheduledPredicted.predictedTrip))
            ),
            actual(includeSchedules = true, includePredictions = true)
        )
    }

    @Test
    fun `StopDetailsDepartures sorts by route preference order`() {
        val objects = ObjectCollectionBuilder()

        val routePinned = objects.route { sortOrder = 100 }
        val routePattern1 =
            objects.routePattern(routePinned) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }

        val routeNotPinned = objects.route { sortOrder = 1 }
        val routeNotPinnedPattern =
            objects.routePattern(routeNotPinned) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val routeNotPinned2 = objects.route { sortOrder = 2 }
        val routeNotPinnedPattern2 =
            objects.routePattern(routeNotPinned2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "C" }
            }

        val stop = objects.stop()

        val time1 = Instant.parse("2024-04-02T16:29:22Z")

        assertEquals(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        routePinned,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                routePinned,
                                "A",
                                null,
                                listOf(routePattern1),
                                listOf()
                            ),
                        )
                    ),
                    PatternsByStop(
                        routeNotPinned,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                routeNotPinned,
                                "B",
                                null,
                                listOf(routeNotPinnedPattern),
                                listOf()
                            ),
                        )
                    ),
                    PatternsByStop(
                        routeNotPinned2,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                routeNotPinned2,
                                "C",
                                null,
                                listOf(routeNotPinnedPattern2),
                                listOf()
                            ),
                        )
                    )
                )
            ),
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(
                        stop.id to
                            listOf(
                                routePattern1.id,
                                routeNotPinnedPattern2.id,
                                routeNotPinnedPattern.id
                            )
                    )
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                null,
                setOf(routePinned.id),
                filterAtTime = time1,
            )
        )
    }

    @Test
    fun `StopDetailsDepartures picks out alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }
        val routePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }

        val departures =
            StopDetailsDepartures(
                stop,
                GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id))),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                emptySet(),
                time
            )

        assertEquals(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                route,
                                "A",
                                null,
                                listOf(routePattern),
                                emptyList(),
                                alertsHere = listOf(alert)
                            )
                        )
                    )
                )
            ),
            departures
        )
    }
}
