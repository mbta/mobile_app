package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ScheduleResponseTest {
    @Test
    fun `getSchedulesTodayByPattern determines which patterns have service today`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePatternA =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePatternB =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val trip1 = objects.trip(routePatternA)

        val time = Instant.parse("2024-03-14T12:23:44-04:00")

        objects.schedule {
            trip = trip1
            stopId = stop.id
            stopSequence = 90
            departureTime = time - 2.hours
        }

        val hasSchedulesToday = ScheduleResponse(objects).getSchedulesTodayByPattern()

        assertTrue(hasSchedulesToday[routePatternA.id]!!)
        assertNull(hasSchedulesToday[routePatternB.id])
    }
}
