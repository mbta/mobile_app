package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.assertNull

class StopDetailsDeparturesTest {
    @Test
    fun `fromData finds trips`() = parametricTest {
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
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                setOf(),
                filterAtTime = time1,
                useTripHeadsigns = anyBoolean(),
            )
        )
    }

    @Test
    fun `fromData finds trips for line`() = parametricTest {
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
            StopDetailsDepartures.fromData(
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
                AlertsStreamDataResponse(objects),
                setOf(),
                filterAtTime = time,
                useTripHeadsigns = anyBoolean(),
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
                        listOf(Direction("West", null, 0), directionEast),
                        emptyList()
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
            checkNotNull(departures).upcomingPatternIds
        )
    }

    @Test
    fun `fromData filters vehicles by relevant routes`() = parametricTest {
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
            StopDetailsDepartures.fromData(
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
                AlertsStreamDataResponse(objects),
                setOf(),
                filterAtTime = time,
                useTripHeadsigns = anyBoolean(),
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
            checkNotNull(departures).filterVehiclesByUpcoming(vehicleResponse)
        )
    }

    @Test
    fun `fromData shows partial data and filters after loading`() = parametricTest {
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
                trips: List<UpcomingTrip>,
                hasSchedulesToday: Boolean = true,
                allDataLoaded: Boolean = true
            ) =
                RealtimePatterns.ByHeadsign(
                    route,
                    headsign,
                    null,
                    listOf(routePattern),
                    trips,
                    alertsHere = emptyList(),
                    emptyList(),
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
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(objects, mapOf(stop.id to objects.routePatterns.keys.toList())),
                ScheduleResponse(objects).takeIf { includeSchedules },
                PredictionsStreamDataResponse(objects).takeIf { includePredictions },
                AlertsStreamDataResponse(objects),
                setOf(),
                filterAtTime = time,
                useTripHeadsigns = anyBoolean(),
            )

        assertEquals(null, actual(includeSchedules = false, includePredictions = false))

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

        assertEquals(null, actual(includeSchedules = true, includePredictions = false))

        assertEquals(
            expected(
                scheduledPredicted.patternsByHeadsign(
                    listOf(scheduledPredicted.scheduledTrip!!, scheduledPredicted.predictedTrip)
                ),
                scheduledUnpredicted.patternsByHeadsign(
                    listOf(scheduledUnpredicted.scheduledTrip!!)
                ),
                unscheduledPredicted.patternsByHeadsign(
                    listOf(unscheduledPredicted.predictedTrip),
                    hasSchedulesToday = false
                )
            ),
            actual(includeSchedules = true, includePredictions = true)
        )
    }

    @Test
    fun `fromData keeps late patterns but drops early patterns after loading`() = parametricTest {
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
                listOf(UpcomingTrip(lateTrip, prediction = latePrediction, predictionStop = stop)),
                allDataLoaded = false
            )
        val expectedLateAfterLoad =
            RealtimePatterns.ByHeadsign(
                route,
                "Late",
                null,
                listOf(latePattern),
                listOf(UpcomingTrip(lateTrip, lateSchedule, latePrediction, predictionStop = stop)),
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
                        emptyList(),
                    )
                )
            )
        val actualBeforeLoaded =
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(objects, mapOf(stop.id to listOf(earlyPattern.id, latePattern.id))),
                null,
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                emptySet(),
                now,
                useTripHeadsigns = anyBoolean(),
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
                        listOf(Direction("", "", 0), Direction("", "", 1)),
                        emptyList(),
                    )
                )
            )
        val actualAfterLoaded =
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(objects, mapOf(stop.id to listOf(earlyPattern.id, latePattern.id))),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                setOf(),
                now,
                useTripHeadsigns = anyBoolean(),
            )
        assertEquals(expectedAfterLoaded, actualAfterLoaded)
    }

    @Test
    fun `fromData sorts by route preference order`() = parametricTest {
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
                                upcomingTrips = emptyList(),
                                hasSchedulesToday = false
                            ),
                        )
                    )
                )
            ),
            StopDetailsDepartures.fromData(
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
                AlertsStreamDataResponse(objects),
                setOf(routePinned.id),
                filterAtTime = time,
                useTripHeadsigns = anyBoolean(),
            )
        )
    }

    @Test
    fun `fromData picks out alerts by platform`() = parametricTest {
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
            StopDetailsDepartures.fromData(
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
                time,
                useTripHeadsigns = anyBoolean(),
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
                                listOf(alert),
                                emptyList()
                            ),
                            RealtimePatterns.ByHeadsign(
                                route,
                                "B",
                                null,
                                listOf(routePattern2),
                                emptyList(),
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
    fun `fromData shows minor alerts`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }
        val routePattern =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip {
                    headsign = "A"
                    stopIds = listOf(stop.id)
                }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        objects.schedule {
            this.trip = objects.trip(routePattern)
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
                effect = Alert.Effect.TrackChange
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }

        val departures =
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern.id))
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                emptySet(),
                time,
                useTripHeadsigns = anyBoolean(),
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
                                listOf(alert),
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
    fun `fromData hides track change at core station`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-north" }
        val route = objects.route { sortOrder = 1 }
        val routePattern =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip {
                    headsign = "A"
                    stopIds = listOf(stop.id)
                }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        objects.schedule {
            this.trip = objects.trip(routePattern)
            stopId = stop.id
            departureTime = time.minus(1.hours)
            stopSequence = 4
        }

        objects.alert {
            activePeriod(
                Instant.parse("2024-03-18T04:30:00-04:00"),
                Instant.parse("2024-03-22T02:30:00-04:00")
            )
            effect = Alert.Effect.TrackChange
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board),
                route = route.id,
                routeType = route.type,
                stop = stop.id
            )
        }

        val departures =
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern.id))
                ),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                emptySet(),
                time,
                useTripHeadsigns = anyBoolean(),
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
    fun `autoStopFilter provides a default StopDetailsFilter given a single route and direction`() =
        parametricTest {
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
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id))),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                StopDetailsFilter(routeId = route.id, directionId = routePattern.directionId, autoFilter = true),
                checkNotNull(departures).autoStopFilter()
            )
    }

    @Test
    fun `autoStopFilter provides a null stop filter value given multiple routes and directions`() =
        parametricTest {
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
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(null, checkNotNull(departures).autoStopFilter())
        }

    @Test
    fun `autoTripFilter provides a trip filter with the first trip selected`() =
        parametricTest {
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
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)
            val vehicle = objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
                vehicleId = vehicle.id
            }

            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip2.id, vehicle.id, 0, false),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route2.id, routePattern2.directionId), null, time)
            )
        }

    @Test
    fun `autoTripFilter provides a null trip filter when no stop filter exists`() =
        parametricTest {
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
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(null, checkNotNull(departures).autoTripFilter(null, null, time))
        }

    @Test
    fun `autoTripFilter provides a null trip filter when no trips exists`() =
        parametricTest {
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
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(null, checkNotNull(departures)
                .autoTripFilter(StopDetailsFilter(
                    route1.id, routePattern1.directionId), null, time
                ))
        }

    @Test
    fun `autoTripFilter provides current trip filter when trip is still upcoming`() =
        parametricTest {
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
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)
            val trip3 = objects.trip(routePattern2)
            val vehicle = objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(9.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(9.minutes)
                vehicleId = vehicle.id
            }

            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip3.id, vehicle.id, 0, false),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route2.id, routePattern2.directionId),
                    TripDetailsFilter(trip3.id, vehicle.id, 0, false),
                    time
                )
            )
        }

    @Test
    fun `autoTripFilter provides next trip when current trip has passed the stop`() =
        parametricTest {
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
            val trip0 = objects.trip(routePattern2)
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)

            val vehicle0 = objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            val vehicle1 = objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
                vehicleId = vehicle0.id
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
                vehicleId = vehicle1.id
            }


            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip2.id, vehicle1.id, 0, false),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route2.id, routePattern2.directionId),
                    TripDetailsFilter(trip0.id, vehicle0.id, 0, false),
                    time
                )
            )
        }

    @Test
    fun `autoTripFilter provides current trip when current trip has passed the stop and is locked`() =
        parametricTest {
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
            val trip0 = objects.trip(routePattern2)
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)

            val vehicle0 = objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            val vehicle1 = objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
                vehicleId = vehicle0.id
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
                vehicleId = vehicle1.id
            }


            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip0.id, vehicle0.id, 0, true),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route2.id, routePattern2.directionId),
                    TripDetailsFilter(trip0.id, vehicle0.id, 0, true),
                    time
                )
            )
        }

    @Test
    fun `autoTripFilter skips cancelled trips`() =
        parametricTest {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route {
                type = RouteType.COMMUTER_RAIL
            }

            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip { headsign = "A" }
                }

            val time = Instant.parse("2024-03-19T14:16:17-04:00")
            val trip1 = objects.trip(routePattern)
            val trip2 = objects.trip(routePattern)
            val trip3 = objects.trip(routePattern)
            val trip4 = objects.trip(routePattern)
            val vehicle = objects.vehicle {
                tripId = trip3.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(7.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip4
                departureTime = time.plus(20.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(10.minutes)
                vehicleId = vehicle.id
            }

            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip3.id, vehicle.id, 0, false),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route.id, routePattern.directionId), null, time)
            )
        }

    @Test
    fun `autoTripFilter selects the first cancelled trip if there are only cancelled trips`() =
        parametricTest {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route {
                type = RouteType.FERRY
            }

            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }

            val time = Instant.parse("2024-03-19T14:16:17-04:00")
            val trip1 = objects.trip(routePattern)
            val trip2 = objects.trip(routePattern)
            val trip3 = objects.trip(routePattern)
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(7.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }

            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                TripDetailsFilter(trip1.id, null, 0, false),
                checkNotNull(departures).autoTripFilter(
                    StopDetailsFilter(route.id, routePattern.directionId), null, time)
            )
        }

    @Test
    fun `stopDetailsFormattedTrips provides upcoming trips in a route and direction`() =
        parametricTest {
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
            val trip0 = objects.trip(routePattern2)
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)

            val vehicle0 = objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            val vehicle1 = objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            val schedule2 = objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
                vehicleId = vehicle0.id
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            val prediction2 = objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
                vehicleId = vehicle1.id
            }

            val departures =
                StopDetailsDepartures.fromData(
                    stop,
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id))
                    ),
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    emptySet(),
                    time,
                    useTripHeadsigns = anyBoolean(),
                )

            assertEquals(
                listOf(
                    TripAndFormat(
                        UpcomingTrip(
                            trip2, schedule2, prediction2, stop, vehicle1
                        ),
                        RealtimePatterns.Format.Some.FormatWithId(
                            trip2.id, route1.type, TripInstantDisplay.Minutes(minutes = 5))
                    )
                ),
                checkNotNull(departures).stopDetailsFormattedTrips(
                    route2.id, routePattern2.directionId, time)
            )
        }

    @Test
    fun `getNoPredictionsStatus resolves service ended`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf()
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "B",
                null,
                listOf(routePattern2),
                listOf()
            )
        )
        assertEquals(
            RealtimePatterns.NoTripsFormat.ServiceEndedToday,
            StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
        )
    }

    @Test
    fun `getNoPredictionsStatus resolves no scheduled service`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf(),
                hasSchedulesToday = false
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "B",
                null,
                listOf(routePattern2),
                listOf(),
                hasSchedulesToday = false
            )
        )
        assertEquals(
            RealtimePatterns.NoTripsFormat.NoSchedulesToday,
            StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
        )
    }

    @Test
    fun `getNoPredictionsStatus resolves a combination of no scheduled service and service ended`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "A" }
            }
        val routePattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf()
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "B",
                null,
                listOf(routePattern2),
                listOf(),
                hasSchedulesToday = false
            )
        )
        assertEquals(
            RealtimePatterns.NoTripsFormat.ServiceEndedToday,
            StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
        )
    }

    @Test
    fun `getNoPredictionsStatus resolves predictions unavailable`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.LIGHT_RAIL }

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "A" }
            }
        val routePattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val routePattern3 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "C" }
            }

        val trip = objects.trip(routePattern3)
        val schedule = objects.schedule {
            tripId = trip.id
            routeId = route.id
            departureTime = now.plus(10.minutes)
        }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf()
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "B",
                null,
                listOf(routePattern2),
                listOf(),
                hasSchedulesToday = false
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "C",
                null,
                listOf(routePattern3),
                listOf(UpcomingTrip(trip, schedule))
            )
        )
        assertEquals(
            RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
            StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
        )
    }

    @Test
    fun `getNoPredictionsStatus resolves null when not loaded`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.LIGHT_RAIL }

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "A" }
            }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf(),
                allDataLoaded = false
            )
        )
        assertNull(StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now))
    }

    @Test
    fun `getNoPredictionsStatus resolves null when predictions exist`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.LIGHT_RAIL }

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "A" }
            }

        val trip = objects.trip(routePattern1)
        val schedule = objects.schedule {
            tripId = trip.id
            routeId = route.id
            departureTime = now.plus(10.minutes)
        }
        val prediction = objects.prediction {
            tripId = trip.id
            routeId = route.id
            departureTime = now.plus(9.minutes)
        }

        val realtimePatterns = listOf(
            RealtimePatterns.ByHeadsign(
                route,
                "A",
                null,
                listOf(routePattern1),
                listOf(UpcomingTrip(trip, schedule, prediction))
            )
        )
        assertNull(StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now))
    }

    @Test
    fun `getNoPredictionsStatus resolves with ByDirection`() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val line = objects.line()

        val routePattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val realtimePatterns = listOf(
            RealtimePatterns.ByDirection(
                line,
                listOf(route),
                Direction(routePattern1.directionId, route),
                listOf(routePattern1),
                listOf()
            ),
            RealtimePatterns.ByHeadsign(
                route,
                "B",
                null,
                listOf(routePattern2),
                listOf()
            )
        )
        assertEquals(
            RealtimePatterns.NoTripsFormat.ServiceEndedToday,
            StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
        )
    }
}
