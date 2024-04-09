package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
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
                    PatternsByHeadsign(
                        route,
                        "Ashmont",
                        listOf(routePatternAshmont),
                        listOf(upcomingTripAshmont1, upcomingTripAshmont2)
                    ),
                    PatternsByHeadsign(
                        route,
                        "Braintree",
                        listOf(routePatternBraintree),
                        listOf(upcomingTripBraintree1, upcomingTripBraintree2)
                    )
                )
            )

        assertEquals(
            listOf(
                PatternsByHeadsign(
                    route,
                    "Ashmont",
                    listOf(routePatternAshmont),
                    listOf(upcomingTripAshmont1)
                ),
                PatternsByHeadsign(
                    route,
                    "Braintree",
                    listOf(routePatternBraintree),
                    listOf(upcomingTripBraintree1)
                ),
                PatternsByHeadsign(
                    route,
                    "Braintree",
                    listOf(routePatternBraintree),
                    listOf(upcomingTripBraintree2)
                ),
                PatternsByHeadsign(
                    route,
                    "Ashmont",
                    listOf(routePatternAshmont),
                    listOf(upcomingTripAshmont2)
                ),
            ),
            patternsByStop.splitPerTrip()
        )
    }
}
