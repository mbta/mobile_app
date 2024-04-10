package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
