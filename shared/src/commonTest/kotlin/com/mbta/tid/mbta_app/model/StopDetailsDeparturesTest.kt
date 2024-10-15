package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
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

        objects.schedule {
            this.trip = objects.trip(routePattern2)
            stopId = stop.id
            departureTime = time1.minus(1.hours)
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
                id = "test-hs"
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

        val departures =
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
                        listOf(directionWest, directionEast)
                    )
                )
            ),
            departures
        )

        assertEquals(
            setOf(
                routePatternB1.id,
                routePatternC1.id,
                routePatternE1.id,
                routePatternB2.id,
                routePatternC2.id,
                routePatternE2.id
            ),
            departures.upcomingPatternIds
        )
    }

    @Test
    fun `StopDetailsDepartures filters vehicles by relevant routes`() {
        val objects = ObjectCollectionBuilder()

        val stop = objects.stop()
        objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternB =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val tripB = objects.trip(routePatternB)

        val routeC =
            objects.route {
                id = "C"
                sortOrder = 2
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternC =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val tripC = objects.trip(routePatternC)

        val routeD =
            objects.route {
                id = "D"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Riverside", "Park St & North")
            }

        val routeE =
            objects.route {
                id = "E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Park St & North")
            }
        val routePatternE =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Heath Street" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                id = "test-hs"
            }
        val tripE = objects.trip(routePatternE)

        val time = Instant.parse("2024-03-18T10:41:13-04:00")

        val schedB =
            objects.schedule {
                trip = tripB
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val schedC =
            objects.schedule {
                trip = tripC
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }
        val schedE =
            objects.schedule {
                trip = tripE
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }

        objects.prediction(schedB) { departureTime = time + 1.5.minutes }
        objects.prediction(schedC) { departureTime = time + 2.3.minutes }
        objects.prediction(schedE) { departureTime = time + 2.3.minutes }

        val departures =
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(
                        stop.id to
                            listOf(
                                routePatternB.id,
                                routePatternC.id,
                                routePatternE.id,
                            )
                    )
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                null,
                setOf(),
                filterAtTime = time,
            )
        val vehicleB =
            objects.vehicle {
                routeId = routeB.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleC =
            objects.vehicle {
                routeId = routeC.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleD =
            objects.vehicle {
                routeId = routeD.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleE =
            objects.vehicle {
                routeId = routeE.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleResponse =
            VehiclesStreamDataResponse(
                mapOf(
                    vehicleB.id to vehicleB,
                    vehicleC.id to vehicleC,
                    vehicleD.id to vehicleD,
                    vehicleE.id to vehicleE,
                )
            )
        assertEquals(
            mapOf(
                vehicleB.id to vehicleB,
                vehicleC.id to vehicleC,
                vehicleE.id to vehicleE,
            ),
            departures.filterVehiclesByUpcoming(vehicleResponse)
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
            fun patternsByHeadsign(
                trips: List<UpcomingTrip>?,
                hasSchedulesToday: Boolean = true,
                allDataLoaded: Boolean = true
            ) =
                RealtimePatterns.ByHeadsign(
                    route,
                    headsign,
                    null,
                    listOf(routePattern),
                    trips,
                    null,
                    hasSchedulesToday,
                    allDataLoaded
                )
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
                    id = "test-$headsign"
                    representativeTrip {
                        this.headsign = headsign
                        this.routePatternId = "test-$headsign"
                    }
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
                scheduledPredicted.patternsByHeadsign(null, allDataLoaded = false),
                scheduledUnpredicted.patternsByHeadsign(null, allDataLoaded = false),
                unscheduledPredicted.patternsByHeadsign(null, allDataLoaded = false),
                unscheduledUnpredicted.patternsByHeadsign(null, allDataLoaded = false)
            ),
            actual(includeSchedules = false, includePredictions = false)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(
                    listOf(scheduledPredicted.predictedTrip!!),
                    allDataLoaded = false
                ),
                unscheduledPredicted.patternsByHeadsign(
                    listOf(unscheduledPredicted.predictedTrip!!),
                    allDataLoaded = false
                ),
                scheduledUnpredicted.patternsByHeadsign(emptyList(), allDataLoaded = false),
                unscheduledUnpredicted.patternsByHeadsign(emptyList(), allDataLoaded = false)
            ),
            actual(includeSchedules = false, includePredictions = true)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(
                    listOf(scheduledPredicted.scheduledTrip!!),
                    allDataLoaded = false
                ),
                scheduledUnpredicted.patternsByHeadsign(
                    listOf(scheduledUnpredicted.scheduledTrip!!),
                    allDataLoaded = false
                ),
                unscheduledPredicted.patternsByHeadsign(
                    emptyList(),
                    hasSchedulesToday = false,
                    allDataLoaded = false
                ),
                unscheduledUnpredicted.patternsByHeadsign(
                    emptyList(),
                    hasSchedulesToday = false,
                    allDataLoaded = false
                )
            ),
            actual(includeSchedules = true, includePredictions = false)
        )

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(
                    listOf(scheduledPredicted.scheduledTrip, scheduledPredicted.predictedTrip)
                ),
                scheduledUnpredicted.patternsByHeadsign(listOf(scheduledUnpredicted.scheduledTrip)),
                unscheduledPredicted.patternsByHeadsign(
                    listOf(unscheduledPredicted.predictedTrip),
                    hasSchedulesToday = false
                )
            ),
            actual(includeSchedules = true, includePredictions = true)
        )
    }

    @Test
    fun `StopDetailsDepartures keeps late patterns but drops early patterns after loading`() {
        val now = Instant.parse("2024-08-16T10:32:38-04:00")
        val late = Instant.parse("2024-08-16T20:00:00-04:00")

        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop()
        val earlyPattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Early" }
            }
        val latePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Late" }
            }

        // since predictions get removed once they're far enough in the past, there will be no
        // prediction for the early pattern, and the past schedule will be filtered out
        objects.schedule {
            this.trip = objects.trip(earlyPattern)
            stopId = stop.id
            departureTime = now.minus(10.minutes)
        }

        val lateTrip = objects.trip(latePattern)
        val latePrediction =
            objects.prediction {
                trip = lateTrip
                stopId = stop.id
                departureTime = late
            }
        val lateSchedule =
            objects.schedule {
                this.trip = lateTrip
                stopId = stop.id
                departureTime = now.minus(10.minutes)
            }

        val expectedEarly =
            RealtimePatterns.ByHeadsign(
                route,
                "Early",
                null,
                listOf(earlyPattern),
                emptyList(),
                allDataLoaded = false
            )
        val expectedLateBeforeLoad =
            RealtimePatterns.ByHeadsign(
                route,
                "Late",
                null,
                listOf(latePattern),
                listOf(UpcomingTrip(lateTrip, latePrediction)),
                allDataLoaded = false
            )
        val expectedLateAfterLoad =
            RealtimePatterns.ByHeadsign(
                route,
                "Late",
                null,
                listOf(latePattern),
                listOf(UpcomingTrip(lateTrip, lateSchedule, latePrediction)),
            )

        val expectedBeforeLoaded =
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        listOf(route),
                        null,
                        stop,
                        listOf(expectedLateBeforeLoad, expectedEarly),
                        listOf(Direction("", "", 0), Direction("", "", 1)),
                    )
                )
            )
        val actualBeforeLoaded =
            StopDetailsDepartures(
                stop,
                GlobalResponse(objects, mapOf(stop.id to listOf(earlyPattern.id, latePattern.id))),
                null,
                PredictionsStreamDataResponse(objects),
                null,
                emptySet(),
                now
            )
        assertEquals(expectedBeforeLoaded, actualBeforeLoaded)

        val expectedAfterLoaded =
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        listOf(route),
                        null,
                        stop,
                        listOf(expectedLateAfterLoad),
                        listOf(Direction("", "", 0), Direction("", "", 1))
                    )
                )
            )
        val actualAfterLoaded =
            StopDetailsDepartures(
                stop,
                GlobalResponse(objects, mapOf(stop.id to listOf(earlyPattern.id, latePattern.id))),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                null,
                setOf(),
                now
            )
        assertEquals(expectedAfterLoaded, actualAfterLoaded)
    }

    @Test
    fun `StopDetailsDepartures sorts by route preference order`() {
        val objects = ObjectCollectionBuilder()

        val time = Instant.parse("2024-04-02T16:29:22Z")

        val stop = objects.stop()
        val routePinned = objects.route { sortOrder = 100 }
        val routePattern1 =
            objects.routePattern(routePinned) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        objects.schedule {
            this.trip = objects.trip(routePattern1)
            stopId = stop.id
            departureTime = time.minus(1.hours)
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
        objects.schedule {
            this.trip = objects.trip(routeNotPinnedPattern2)
            stopId = stop.id
            departureTime = time.minus(1.hours)
        }

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
                                listOf(),
                                null,
                                false
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
                filterAtTime = time,
            )
        )
    }

    @Test
    fun `StopDetailsDepartures picks out alerts by platform`() {
        val objects = ObjectCollectionBuilder()
        lateinit var platform1: Stop
        lateinit var platform2: Stop
        val stop =
            objects.stop {
                platform1 = childStop()
                platform2 = childStop()
            }
        val route = objects.route { sortOrder = 1 }
        val routePattern1 =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip {
                    headsign = "A"
                    stopIds = listOf(platform1.id)
                }
            }
        val routePattern2 =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip {
                    headsign = "B"
                    stopIds = listOf(platform2.id)
                }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        objects.schedule {
            this.trip = objects.trip(routePattern1)
            stopId = stop.id
            departureTime = time.minus(1.hours)
            stopSequence = 4
        }
        objects.schedule {
            this.trip = objects.trip(routePattern2)
            stopId = stop.id
            departureTime = time.minus(1.hours)
            stopSequence = 4
        }

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
                    stop = platform1.id
                )
            }

        val departures =
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(
                        platform1.id to listOf(routePattern1.id),
                        platform2.id to listOf(routePattern2.id)
                    )
                ),
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
                                listOf(routePattern1),
                                emptyList(),
                                listOf(alert)
                            ),
                            RealtimePatterns.ByHeadsign(
                                route,
                                "B",
                                null,
                                listOf(routePattern2),
                                emptyList(),
                                emptyList()
                            )
                        )
                    )
                )
            ),
            departures
        )
    }

    @Test
    fun `StopDetailsDepartues provides a default StopDetailsFilter given a single route and direction`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val time = Instant.parse("2024-03-19T14:16:17-04:00")

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
            StopDetailsFilter(routeId = route.id, directionId = routePattern.directionId),
            departures.autoFilter()
        )
    }

    @Test
    fun `StopDetailsDepartures provides a null filter value given multiple routes and directions`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()
        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val departures =
            StopDetailsDepartures(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                emptySet(),
                time
            )

        assertEquals(null, departures.autoFilter())
    }
}
