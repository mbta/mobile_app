package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class PatternsByHeadsignTest {
    @Test
    fun `formats as loading when null trips`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        assertEquals(
            PatternsByHeadsign.Format.Loading,
            PatternsByHeadsign(route, "", emptyList(), null, null).format(now)
        )
    }

    @Test
    fun `formats as alert with no trips and alert`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val alert = objects.alert {}

        assertEquals(
            PatternsByHeadsign.Format.NoService(alert),
            PatternsByHeadsign(route, "", emptyList(), emptyList(), listOf(alert)).format(now)
        )
    }

    @Test
    fun `formats as alert with trip and alert`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert {}

        assertEquals(
            PatternsByHeadsign.Format.NoService(alert),
            PatternsByHeadsign(route, "", emptyList(), listOf(upcomingTrip), listOf(alert))
                .format(now)
        )
    }

    @Test
    fun `formats as none with no trips and no alert`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        assertEquals(
            PatternsByHeadsign.Format.None,
            PatternsByHeadsign(route, "", emptyList(), emptyList(), emptyList()).format(now)
        )
    }

    @Test
    fun `skips trips that should be hidden`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)

        assertEquals(
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        trip2.id,
                        UpcomingTrip.Format.Minutes(5)
                    )
                )
            ),
            PatternsByHeadsign(route, "", emptyList(), listOf(upcomingTrip1, upcomingTrip2))
                .format(now)
        )
    }

    @Test
    fun `format skips schedules on subway but keeps on non-subway`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val subwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val busRoute = objects.route { type = RouteType.BUS }

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 5.minutes
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)

        assertEquals(
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        trip2.id,
                        UpcomingTrip.Format.Minutes(5)
                    )
                )
            ),
            PatternsByHeadsign(subwayRoute, "", emptyList(), listOf(upcomingTrip1, upcomingTrip2))
                .format(now)
        )
        assertEquals(
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        trip1.id,
                        UpcomingTrip.Format.Schedule(now + 5.minutes)
                    ),
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        trip2.id,
                        UpcomingTrip.Format.Minutes(5)
                    )
                )
            ),
            PatternsByHeadsign(busRoute, "", emptyList(), listOf(upcomingTrip1, upcomingTrip2))
                .format(now)
        )
    }

    @Test
    fun `directionId finds trips`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip0 = objects.trip { directionId = 0 }
        val prediction0 = objects.schedule { trip = trip0 }
        val trip1 = objects.trip { directionId = 1 }
        val prediction1 = objects.schedule { trip = trip1 }
        assertEquals(
            0,
            PatternsByHeadsign(route, "", emptyList(), listOf(objects.upcomingTrip(prediction0)))
                .directionId()
        )
        assertEquals(
            1,
            PatternsByHeadsign(route, "", emptyList(), listOf(objects.upcomingTrip(prediction1)))
                .directionId()
        )
    }

    @Test
    fun `directionId finds patterns`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern0 = objects.routePattern(route) { directionId = 0 }
        val routePattern1 = objects.routePattern(route) { directionId = 1 }
        assertEquals(0, PatternsByHeadsign(route, "", listOf(routePattern0)).directionId())
        assertEquals(1, PatternsByHeadsign(route, "", listOf(routePattern1)).directionId())
    }

    @Test
    fun `directionId throws if empty`() {
        assertFailsWith<NoSuchElementException> {
            PatternsByHeadsign(ObjectCollectionBuilder.Single.route(), "", emptyList())
                .directionId()
        }
    }
}
