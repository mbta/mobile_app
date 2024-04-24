package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
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
                            PatternsByHeadsign(
                                route,
                                "A",
                                listOf(routePattern1),
                                listOf(
                                    objects.upcomingTrip(schedule1, prediction1),
                                    objects.upcomingTrip(schedule2)
                                )
                            ),
                            PatternsByHeadsign(route, "B", listOf(routePattern2), listOf())
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
                filterAtTime = time1
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
                PatternsByHeadsign(route, headsign, listOf(routePattern), trips)
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

        fun expected(vararg pattern: PatternsByHeadsign): StopDetailsDepartures =
            StopDetailsDepartures(listOf(PatternsByStop(route, stop, pattern.toList())))

        fun actual(includeSchedules: Boolean = true, includePredictions: Boolean = true) =
            StopDetailsDepartures(
                stop,
                GlobalResponse(objects, mapOf(stop.id to objects.routePatterns.keys.toList())),
                ScheduleResponse(objects).takeIf { includeSchedules },
                PredictionsStreamDataResponse(objects).takeIf { includePredictions },
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
}
