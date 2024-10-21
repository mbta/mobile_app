package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock

class PatternsByStopTest {
    @Test
    fun `splitPerTrip divides and sorts properly`() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        val stop = objects.stop()
        val routePatternAshmont =
            objects.routePattern(route) { representativeTrip { headsign = "Ashmont" } }
        val routePatternBraintree =
            objects.routePattern(route) { representativeTrip { headsign = "Braintree" } }

        val time = Clock.System.now()

        val tripAshmont1 = objects.trip(routePatternAshmont)
        val predictionAshmont1 =
            objects.prediction {
                trip = tripAshmont1
                departureTime = time + 1.minutes
            }
        val upcomingTripAshmont1 = objects.upcomingTrip(predictionAshmont1)

        val tripBraintree1 = objects.trip(routePatternBraintree)
        val scheduleBraintree1 =
            objects.schedule {
                trip = tripBraintree1
                departureTime = time + 2.minutes
            }
        val predictionBraintree1 =
            objects.prediction(scheduleBraintree1) { departureTime = time + 1.9.minutes }
        val upcomingTripBraintree1 = objects.upcomingTrip(scheduleBraintree1, predictionBraintree1)

        val tripBraintree2 = objects.trip(routePatternBraintree)
        val predictionBraintree2 =
            objects.prediction {
                trip = tripBraintree2
                departureTime = time + 6.minutes
            }
        val upcomingTripBraintree2 = objects.upcomingTrip(predictionBraintree2)

        val tripAshmont2 = objects.trip(routePatternAshmont)
        val scheduleAshmont2 =
            objects.schedule {
                trip = tripAshmont2
                departureTime = time + 10.minutes
            }
        val upcomingTripAshmont2 = objects.upcomingTrip(scheduleAshmont2)

        val patternsByStop =
            PatternsByStop(
                route,
                stop,
                listOf(
                    RealtimePatterns.ByHeadsign(
                        route,
                        "Ashmont",
                        null,
                        listOf(routePatternAshmont),
                        listOf(upcomingTripAshmont1, upcomingTripAshmont2)
                    ),
                    RealtimePatterns.ByHeadsign(
                        route,
                        "Braintree",
                        null,
                        listOf(routePatternBraintree),
                        listOf(upcomingTripBraintree1, upcomingTripBraintree2)
                    )
                )
            )

        assertEquals(
            listOf(
                upcomingTripAshmont1,
                upcomingTripBraintree1,
                upcomingTripBraintree2,
                upcomingTripAshmont2
            ),
            patternsByStop.allUpcomingTrips()
        )
    }

    @Test
    fun `alertsHereFor returns directional alerts`() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        val stop = objects.stop()
        val routePatternAshmont =
            objects.routePattern(route) {
                representativeTrip {
                    headsign = "Ashmont"
                    stopIds = listOf(stop.id)
                }
            }
        val routePatternBraintree =
            objects.routePattern(route) {
                representativeTrip {
                    headsign = "Braintree"
                    stopIds = listOf(stop.id)
                }
            }

        val time = Clock.System.now()

        val tripBraintree = objects.trip(routePatternBraintree)
        val scheduleBraintree =
            objects.schedule {
                trip = tripBraintree
                departureTime = time + 2.minutes
            }
        val upcomingTripBraintree = objects.upcomingTrip(scheduleBraintree)

        val tripAshmont = objects.trip(routePatternAshmont)
        val scheduleAshmont =
            objects.schedule {
                trip = tripAshmont
                departureTime = time + 10.minutes
            }
        val upcomingTripAshmont = objects.upcomingTrip(scheduleAshmont)

        val alert1 =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(time - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit),
                    route = route.id,
                    stop = stop.id
                )
            }
        val alert2 =
            objects.alert {
                effect = Alert.Effect.Shuttle
                activePeriod(time - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = stop.id
                )
            }
        val alert3 =
            objects.alert {
                effect = Alert.Effect.Detour
                activePeriod(time - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = "other stop"
                )
            }

        val patternsByStop =
            PatternsByStop(
                route,
                stop,
                listOf(
                    RealtimePatterns.ByHeadsign(
                        route,
                        "Ashmont",
                        null,
                        listOf(routePatternAshmont),
                        listOf(upcomingTripAshmont),
                        listOf(alert1)
                    ),
                    RealtimePatterns.ByHeadsign(
                        route,
                        "Braintree",
                        null,
                        listOf(routePatternBraintree),
                        listOf(upcomingTripBraintree),
                        listOf(alert2, alert3)
                    )
                )
            )

        val global =
            GlobalResponse(
                objects,
                mapOf(Pair(stop.id, listOf(routePatternAshmont.id, routePatternBraintree.id)))
            )
        val alerts =
            patternsByStop.alertsHereFor(
                directionId = routePatternAshmont.directionId,
                global = global
            )
        assertEquals(listOf(alert2), alerts)
    }

    @Test
    fun `alertsHereFor doesn't duplicate alerts`() {
        val objects = ObjectCollectionBuilder()
        lateinit var platform1: Stop
        lateinit var platform2: Stop
        val station =
            objects.stop {
                platform1 = childStop()
                platform2 = childStop()
            }

        val route = objects.route()
        val pattern1 =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(platform1.id) }
            }
        val pattern2 =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(platform2.id) }
            }
        val pattern3 =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { stopIds = listOf(platform1.id) }
            }
        val pattern4 =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { stopIds = listOf(platform2.id) }
            }

        val alert =
            objects.alert {
                effect = Alert.Effect.Shuttle
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = station.id
                )
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
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = platform2.id
                )
            }

        val patternsByStop =
            PatternsByStop(
                route,
                station,
                listOf(
                    RealtimePatterns.ByHeadsign(
                        route,
                        "Out",
                        null,
                        listOf(pattern1, pattern4),
                        upcomingTrips = emptyList(),
                        alertsHere = listOf(alert)
                    ),
                    RealtimePatterns.ByHeadsign(
                        route,
                        "In",
                        null,
                        listOf(pattern2, pattern3),
                        upcomingTrips = emptyList(),
                        alertsHere = listOf(alert)
                    )
                )
            )

        val global =
            GlobalResponse(
                objects,
                mapOf(
                    platform1.id to listOf(pattern1.id, pattern3.id),
                    platform2.id to listOf(pattern2.id, pattern4.id)
                )
            )
        assertEquals(listOf(alert), patternsByStop.alertsHereFor(0, global))
        assertEquals(listOf(alert), patternsByStop.alertsHereFor(1, global))
    }

    object GroupedGLTestPatterns {
        val objects = ObjectCollectionBuilder()

        val line = objects.line()
        val routeC = objects.route()
        val routeE = objects.route()
        val stop = objects.stop { id = "place-gilmn" }
        val routePatternCCleveland =
            objects.routePattern(routeC) {
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Cleveland Circle" }
            }
        val routePatternCGovCtr =
            objects.routePattern(routeC) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Government Center" }
            }
        val routePatternCMedford =
            objects.routePattern(routeC) {
                directionId = 1
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Medford/Tufts" }
            }
        val routePatternEHeath =
            objects.routePattern(routeE) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Heath Street" }
            }
        val routePatternEMedford =
            objects.routePattern(routeE) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Medford/Tufts" }
            }

        val time = Clock.System.now()

        val tripEHeath1 = objects.trip(routePatternEHeath)
        val predictionEHeath1 =
            objects.prediction {
                trip = tripEHeath1
                departureTime = time + 1.minutes
            }
        val upcomingTripEHeath1 = objects.upcomingTrip(predictionEHeath1)

        val tripCCleveland1 = objects.trip(routePatternCCleveland)
        val scheduleCCleveland1 =
            objects.schedule {
                trip = tripCCleveland1
                departureTime = time + 2.minutes
            }
        val predictionCCleveland1 =
            objects.prediction(scheduleCCleveland1) { departureTime = time + 1.9.minutes }
        val vehicleCCleveland1 =
            objects.vehicle {
                tripId = tripCCleveland1.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
            }
        val upcomingTripCCleveland1 =
            objects.upcomingTrip(scheduleCCleveland1, predictionCCleveland1, vehicleCCleveland1)
        val upcomingTripScheduledCCleveland1 = objects.upcomingTrip(scheduleCCleveland1)

        val tripEMedford1 = objects.trip(routePatternEMedford)
        val predictionEMedford1 =
            objects.prediction {
                trip = tripEMedford1
                departureTime = time + 1.minutes
            }
        val upcomingTripEMedford1 = objects.upcomingTrip(predictionEMedford1)

        val tripCMedford1 = objects.trip(routePatternCMedford)
        val scheduleCMedford1 =
            objects.schedule {
                trip = tripCMedford1
                departureTime = time + 2.minutes
            }
        val predictionCMedford1 =
            objects.prediction(scheduleCMedford1) { departureTime = time + 1.9.minutes }
        val vehicleCMedford1 =
            objects.vehicle {
                tripId = tripCMedford1.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
            }
        val upcomingTripCMedford1 =
            objects.upcomingTrip(scheduleCMedford1, predictionCMedford1, vehicleCMedford1)
        val upcomingTripScheduledCMedford1 = objects.upcomingTrip(scheduleCMedford1)

        val staticPatternsWest =
            NearbyStaticData.StaticPatterns.ByDirection(
                line,
                listOf(routeC, routeE),
                Direction("West", "Copley & West", 0),
                listOf(routePatternCCleveland, routePatternEHeath),
                setOf(stop.id)
            )

        val staticPatternsEast =
            NearbyStaticData.StaticPatterns.ByDirection(
                line,
                listOf(routeC, routeE),
                Direction("East", "Medford/Tufts", 1),
                listOf(routePatternCMedford, routePatternEMedford),
                setOf(stop.id)
            )

        val typicalUpcomingTrips: UpcomingTripsMap =
            mapOf(
                RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                    routeE.id,
                    routePatternEHeath.id,
                    stop.id
                ) to listOf(upcomingTripEHeath1),
                RealtimePatterns.UpcomingTripKey.ByDirection(routeE.id, 0, stop.id) to
                    listOf(upcomingTripEHeath1),
                RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                    routeE.id,
                    routePatternEMedford.id,
                    stop.id
                ) to listOf(upcomingTripEMedford1),
                RealtimePatterns.UpcomingTripKey.ByDirection(routeE.id, 1, stop.id) to
                    listOf(upcomingTripEMedford1),
            )
        val atypicalScheduledTripsMap: UpcomingTripsMap =
            typicalUpcomingTrips +
                mapOf(
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        routeC.id,
                        routePatternCMedford.id,
                        stop.id
                    ) to listOf(upcomingTripScheduledCCleveland1),
                    RealtimePatterns.UpcomingTripKey.ByDirection(routeC.id, 1, stop.id) to
                        listOf(upcomingTripScheduledCCleveland1),
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        routeC.id,
                        routePatternCMedford.id,
                        stop.id
                    ) to listOf(upcomingTripScheduledCMedford1),
                    RealtimePatterns.UpcomingTripKey.ByDirection(routeC.id, 1, stop.id) to
                        listOf(upcomingTripScheduledCMedford1),
                )
        val atypicalUpcomingTripsMap: UpcomingTripsMap =
            typicalUpcomingTrips +
                mapOf(
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        routeC.id,
                        routePatternCCleveland.id,
                        stop.id
                    ) to listOf(upcomingTripCCleveland1),
                    RealtimePatterns.UpcomingTripKey.ByDirection(routeC.id, 0, stop.id) to
                        listOf(upcomingTripCCleveland1),
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        routeC.id,
                        routePatternCMedford.id,
                        stop.id
                    ) to listOf(upcomingTripCMedford1),
                    RealtimePatterns.UpcomingTripKey.ByDirection(routeC.id, 1, stop.id) to
                        listOf(upcomingTripCMedford1),
                )

        val hasSchedules =
            mapOf(
                routePatternCCleveland.id to true,
                routePatternCGovCtr.id to true,
                routePatternCMedford.id to true,
                routePatternEHeath.id to true,
                routePatternEMedford.id to true,
            )

        fun resolveWith(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap?
        ): List<RealtimePatterns> {
            return PatternsByStop.resolveRealtimePatternForDirection(
                staticData,
                upcomingTripsMap,
                stop.id,
                null,
                hasSchedules,
                true
            )
        }
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByHeadsign when only typical trips are predicted with distinct atypical static headsigns in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByHeadsign(
                    data.routeE,
                    "Heath Street",
                    data.line,
                    listOf(data.routePatternEHeath),
                    upcomingTrips = listOf(data.upcomingTripEHeath1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsWest, data.typicalUpcomingTrips)
        )
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByHeadsign when atypical trips are scheduled but not predicted with distinct atypical static headsigns in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByHeadsign(
                    data.routeE,
                    "Heath Street",
                    data.line,
                    listOf(data.routePatternEHeath),
                    upcomingTrips = listOf(data.upcomingTripEHeath1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsWest, data.atypicalScheduledTripsMap)
        )
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByDirection when atypical trips are predicted with distinct headsigns in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByDirection(
                    data.line,
                    listOf(data.routeC, data.routeE),
                    Direction("West", "Copley & West", 0),
                    listOf(data.routePatternCCleveland, data.routePatternEHeath),
                    upcomingTrips = listOf(data.upcomingTripEHeath1, data.upcomingTripCCleveland1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsWest, data.atypicalUpcomingTripsMap)
        )
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByHeadsign when only typical trips are predicted with the same static headsign in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByHeadsign(
                    data.routeE,
                    "Medford/Tufts",
                    data.line,
                    listOf(data.routePatternEMedford),
                    upcomingTrips = listOf(data.upcomingTripEMedford1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsEast, data.typicalUpcomingTrips)
        )
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByHeadsign when atypical trips are scheduled but not predicted with the same static headsign in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByHeadsign(
                    data.routeE,
                    "Medford/Tufts",
                    data.line,
                    listOf(data.routePatternEMedford),
                    upcomingTrips = listOf(data.upcomingTripEMedford1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsEast, data.atypicalScheduledTripsMap)
        )
    }

    @Test
    fun `resolveRealtimePatternForDirection returns a realtime ByDirection when atypical trips are predicted with the same headsign in a direction`() {
        val data = GroupedGLTestPatterns
        assertEquals(
            listOf(
                RealtimePatterns.ByDirection(
                    data.line,
                    listOf(data.routeC, data.routeE),
                    Direction("East", "Medford/Tufts", 1),
                    listOf(data.routePatternCMedford, data.routePatternEMedford),
                    upcomingTrips = listOf(data.upcomingTripEMedford1, data.upcomingTripCMedford1),
                    null,
                    true
                )
            ),
            data.resolveWith(data.staticPatternsEast, data.atypicalUpcomingTripsMap)
        )
    }
}
