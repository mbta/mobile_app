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
}
