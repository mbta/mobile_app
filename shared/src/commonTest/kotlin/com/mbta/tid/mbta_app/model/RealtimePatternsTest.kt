package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class RealtimePatternsTest {
    // trip details doesn't use RealtimePatterns
    private fun ParametricTest.anyContext() =
        anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)

    @Test
    fun `formats as loading when null trips`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        assertEquals(
            RealtimePatterns.Format.Loading,
            RealtimePatterns.ByHeadsign(route, "", null, emptyList(), null, null)
                .format(now, anyContext())
        )
    }

    @Test
    fun `formats as alert with no trips and alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val alert = objects.alert {}

        assertEquals(
            RealtimePatterns.Format.NoService(alert),
            RealtimePatterns.ByHeadsign(route, "", null, emptyList(), emptyList(), listOf(alert))
                .format(now, anyContext())
        )
    }

    @Test
    fun `formats as alert with trip and alert`() = parametricTest {
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
            RealtimePatterns.Format.NoService(alert),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip),
                    listOf(alert)
                )
                .format(now, anyContext())
        )
    }

    @Test
    fun `formats as none with no trips and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        assertEquals(
            RealtimePatterns.Format.None,
            RealtimePatterns.ByHeadsign(route, "", null, emptyList(), emptyList(), emptyList())
                .format(now, anyContext())
        )
    }

    @Test
    fun `skips trips that should be hidden`() = parametricTest {
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
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip2.id,
                        TripInstantDisplay.Minutes(5)
                    )
                )
            ),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, anyContext())
        )
    }

    @Test
    fun `format skips schedules on subway but keeps on non-subway`() = parametricTest {
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
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip2.id,
                        TripInstantDisplay.Minutes(5)
                    )
                )
            ),
            RealtimePatterns.ByHeadsign(
                    subwayRoute,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, anyContext())
        )
        assertEquals(
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip1.id,
                        TripInstantDisplay.Schedule(now + 5.minutes)
                    ),
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip2.id,
                        TripInstantDisplay.Minutes(5)
                    )
                )
            ),
            RealtimePatterns.ByHeadsign(
                    busRoute,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, anyContext())
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
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(objects.upcomingTrip(prediction0))
                )
                .directionId()
        )
        assertEquals(
            1,
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(objects.upcomingTrip(prediction1))
                )
                .directionId()
        )
    }

    @Test
    fun `directionId finds patterns`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern0 = objects.routePattern(route) { directionId = 0 }
        val routePattern1 = objects.routePattern(route) { directionId = 1 }
        assertEquals(
            0,
            RealtimePatterns.ByHeadsign(route, "", null, listOf(routePattern0)).directionId()
        )
        assertEquals(
            1,
            RealtimePatterns.ByHeadsign(route, "", null, listOf(routePattern1)).directionId()
        )
    }

    @Test
    fun `directionId throws if empty`() {
        assertFailsWith<NoSuchElementException> {
            RealtimePatterns.ByHeadsign(
                    ObjectCollectionBuilder.Single.route(),
                    "",
                    null,
                    emptyList()
                )
                .directionId()
        }
    }

    @Test
    fun `predictions grouped by direction are displayed`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val line = objects.line {}
        val route1 = objects.route()
        val route2 = objects.route()
        val route3 = objects.route()

        val trip1 = objects.trip { routeId = route1.id }
        val trip2 = objects.trip { routeId = route2.id }
        val trip3 = objects.trip { routeId = route3.id }
        val trip4 = objects.trip { routeId = route1.id }

        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = now + 3.minutes
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }
        val prediction3 =
            objects.prediction {
                trip = trip3
                departureTime = now + 7.minutes
            }
        val prediction4 =
            objects.prediction {
                trip = trip4
                departureTime = now + 9.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val upcomingTrip3 = objects.upcomingTrip(prediction3)
        val upcomingTrip4 = objects.upcomingTrip(prediction4)

        val directionPatterns =
            RealtimePatterns.ByDirection(
                line,
                routes = listOf(route1, route2, route3),
                direction = Direction("", "", 0),
                emptyList(),
                listOf(upcomingTrip1, upcomingTrip2, upcomingTrip3, upcomingTrip4)
            )

        assertEquals(
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip1.id,
                        TripInstantDisplay.Minutes(3)
                    ),
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip2.id,
                        TripInstantDisplay.Minutes(5)
                    ),
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip3.id,
                        TripInstantDisplay.Minutes(7)
                    )
                )
            ),
            directionPatterns.format(now, anyContext())
        )

        assertEquals(directionPatterns.routesByTrip[trip2.id], route2)
    }

    @Test
    fun `filters applicable alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val validAlert =
            objects.alert {
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
        val invalidAlert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = "wrong",
                    routeType = route.type,
                    stop = "wrong"
                )
            }
        assertEquals(
            RealtimePatterns.applicableAlerts(
                listOf(route),
                setOf(stop.id),
                null,
                listOf(validAlert, invalidAlert)
            ),
            listOf(validAlert)
        )
    }

    @Test
    fun `filters out alerts without Board activity`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }
        assertEquals(
            RealtimePatterns.applicableAlerts(listOf(route), setOf(stop.id), null, listOf(alert)),
            emptyList()
        )
    }

    @Test
    fun `filters out alerts with non-matching route ID`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = "not matching",
                    routeType = route.type,
                    stop = stop.id
                )
            }
        assertEquals(
            RealtimePatterns.applicableAlerts(listOf(route), setOf(stop.id), null, listOf(alert)),
            emptyList()
        )
    }

    @Test
    fun `filters out alerts with non-matching stop ID`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = "not matching"
                )
            }
        assertEquals(
            RealtimePatterns.applicableAlerts(listOf(route), setOf(stop.id), null, listOf(alert)),
            emptyList()
        )
    }
}
