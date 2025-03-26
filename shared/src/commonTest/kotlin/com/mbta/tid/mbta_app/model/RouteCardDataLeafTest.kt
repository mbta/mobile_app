package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class RouteCardDataLeafTest {
    private fun ParametricTest.anyNonScheduleBasedRouteType() =
        anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY)

    @Test
    fun `formats as alert with no trips and major alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                id = "Red"
                type = anyNonScheduleBasedRouteType()
            }

        val alert = objects.alert { effect = Alert.Effect.Suspension }

        assertEquals(
            UpcomingFormat.Disruption(alert, "alert-large-red-suspension"),
            RouteCardData.Leaf(0, emptyList(), emptySet(), emptyList(), listOf(alert), true, true)
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val cases =
            mapOf(
                objects.route { id = "Red" } to "alert-large-red-issue",
                objects.route { id = "Mattapan" } to "alert-large-mattapan-issue",
                objects.route { id = "Orange" } to "alert-large-orange-issue",
                objects.route { id = "Green-B" } to "alert-large-green-issue",
                objects.route { id = "Blue" } to "alert-large-blue-issue",
                objects.route { id = "741" } to "alert-large-silver-issue",
                objects.route { type = RouteType.COMMUTER_RAIL } to "alert-large-commuter-issue",
                objects.route { type = RouteType.FERRY } to "alert-large-ferry-issue",
                objects.route { type = RouteType.BUS } to "alert-large-bus-issue",
                objects.route { type = RouteType.HEAVY_RAIL } to "alert-borderless-issue"
            )

        val alert = objects.alert { effect = Alert.Effect.ServiceChange }

        for ((route, icon) in cases) {
            assertEquals(
                UpcomingFormat.NoTrips(
                    UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                    UpcomingFormat.SecondaryAlert(icon)
                ),
                RouteCardData.Leaf(
                        0,
                        emptyList(),
                        emptySet(),
                        emptyList(),
                        listOf(alert),
                        true,
                        true
                    )
                    .format(now, route, 3, anyEnumValue())
            )
        }
    }

    @Test
    fun `formats as alert with trip and major alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                id = "741"
                type = anyNonScheduleBasedRouteType()
            }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.Suspension }

        assertEquals(
            UpcomingFormat.Disruption(alert, "alert-large-silver-suspension"),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true
                )
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `preserves trip alongside secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.ServiceChange }

        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip,
                        route.type,
                        TripInstantDisplay.Minutes(1)
                    )
                ),
                UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true
                )
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    @Ignore // TODO once downstream alerts are added
    fun `includes downstream alert as secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.Shuttle }

        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip,
                        route.type,
                        TripInstantDisplay.Minutes(1)
                    )
                ),
                UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    emptyList(),
                    true,
                    true
                )
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyNonScheduleBasedRouteType() }

        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday),
            RouteCardData.Leaf(0, emptyList(), emptySet(), emptyList(), emptyList(), true, true)
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `formats as none with subway schedules but no predictions and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyOf(RouteType.LIGHT_RAIL, RouteType.HEAVY_RAIL) }
        val pattern = objects.routePattern(route)
        val schedule =
            objects.schedule {
                trip = objects.trip(pattern)
                departureTime = now + 2.minutes
            }
        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(objects.upcomingTrip(schedule)),
                    emptyList(),
                    true,
                    true
                )
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `formats as loading if empty trips but still loading`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }
        val pattern = objects.routePattern(route)
        assertEquals(
            UpcomingFormat.Loading,
            RouteCardData.Leaf(
                    0,
                    listOf(pattern),
                    emptySet(),
                    emptyList(),
                    emptyList(),
                    false,
                    true
                )
                .format(now, route, 3, anyEnumValue())
        )
    }

    @Test
    fun `skips trips that should be hidden`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyNonScheduleBasedRouteType() }

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
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip2,
                        route.type,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true
                )
                .format(now, route, 3, anyEnumValue())
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
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip2,
                        subwayRoute.type,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true
                )
                .format(now, subwayRoute, 3, anyEnumValue())
        )
        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip1,
                        busRoute.type,
                        TripInstantDisplay.ScheduleMinutes(5)
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip2,
                        busRoute.type,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true
                )
                .format(now, busRoute, 3, anyEnumValue())
        )
    }

    @Test
    fun `format handles no schedules all day`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }

        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday),
            RouteCardData.Leaf(0, emptyList(), emptySet(), emptyList(), listOf(), true, false)
                .format(now, route, 3, anyEnumValue())
        )
    }
}
