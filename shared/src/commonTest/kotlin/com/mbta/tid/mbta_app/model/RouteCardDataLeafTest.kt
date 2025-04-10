package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RouteCardDataLeafTest.RedLine.route
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import com.mbta.tid.mbta_app.utils.fromBostonTime
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime

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
            LeafFormat.Single(null, UpcomingFormat.Disruption(alert, "alert-large-red-suspension")),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    listOf(alert),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
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
                LeafFormat.Single(
                    null,
                    UpcomingFormat.NoTrips(
                        UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                        UpcomingFormat.SecondaryAlert(icon)
                    )
                ),
                RouteCardData.Leaf(
                        0,
                        emptyList(),
                        emptySet(),
                        emptyList(),
                        listOf(alert),
                        true,
                        true,
                        emptyList()
                    )
                    .format(now, route, GlobalResponse(objects), anyEnumValue())
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
            LeafFormat.Single(
                "",
                UpcomingFormat.Disruption(alert, "alert-large-silver-suspension")
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
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
            LeafFormat.Single(
                "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip,
                            route.type,
                            TripInstantDisplay.Minutes(1)
                        )
                    ),
                    UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
                )
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
        )
    }

    @Test
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
            LeafFormat.Single(
                "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip,
                            route.type,
                            TripInstantDisplay.Minutes(1)
                        )
                    ),
                    UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
                )
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    emptyList(),
                    true,
                    true,
                    listOf(alert)
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyNonScheduleBasedRouteType() }

        assertEquals(
            LeafFormat.Single(
                null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday)
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    emptyList(),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
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
            LeafFormat.Single(
                "",
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable)
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(objects.upcomingTrip(schedule)),
                    emptyList(),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
        )
    }

    @Test
    fun `formats as loading if empty trips but still loading`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }
        val pattern = objects.routePattern(route)
        assertEquals(
            LeafFormat.Single(null, UpcomingFormat.Loading),
            RouteCardData.Leaf(
                    0,
                    listOf(pattern),
                    emptySet(),
                    emptyList(),
                    emptyList(),
                    false,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
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
            LeafFormat.Single(
                "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip2,
                            route.type,
                            TripInstantDisplay.Minutes(5)
                        )
                    ),
                    null
                )
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
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
            LeafFormat.Single(
                "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip2,
                            subwayRoute.type,
                            TripInstantDisplay.Minutes(5)
                        )
                    ),
                    null
                )
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList()
                )
                .format(now, subwayRoute, GlobalResponse(objects), anyEnumValue())
        )
        assertEquals(
            LeafFormat.Single(
                "",
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
                )
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList()
                )
                .format(now, busRoute, GlobalResponse(objects), anyEnumValue())
        )
    }

    @Test
    fun `format handles no schedules all day`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }

        assertEquals(
            LeafFormat.Single(
                null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday)
            ),
            RouteCardData.Leaf(
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    listOf(),
                    true,
                    false,
                    emptyList()
                )
                .format(now, route, GlobalResponse(objects), anyEnumValue())
        )
    }

    private object RedLine {
        private val objects = ObjectCollectionBuilder()

        fun objects() = objects.clone()

        val route =
            objects.route {
                directionNames = listOf("South", "North")
                directionDestinations = listOf("Ashmont/Braintree", "Alewife")
                type = RouteType.HEAVY_RAIL
            }

        data class Stop4(
            val parent: Stop,
            val south1: Stop,
            val south2: Stop,
            val north1: Stop,
            val north2: Stop
        )

        private fun stop4(): Stop4 {
            lateinit var south1: Stop
            lateinit var south2: Stop
            lateinit var north1: Stop
            lateinit var north2: Stop
            val parent =
                objects.stop {
                    south1 = childStop()
                    south2 = childStop()
                    north1 = childStop()
                    north2 = childStop()
                }
            return Stop4(parent, south1, south2, north1, north2)
        }

        val jfkUmass = stop4()

        val ashmontSouth =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Ashmont" }
            }

        val braintreeSouth =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Braintree" }
            }

        val ashmontNorth =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Alewife" }
            }

        val braintreeNorth =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Alewife" }
            }

        val global = GlobalResponse(objects)
    }

    @Test
    fun `formats Red Line southbound as branching showing next 3 trips`() = parametricTest {
        val objects = RedLine.objects()
        val now = Clock.System.now()
        val prediction1 =
            objects.prediction {
                arrivalTime = now + 1.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }
        val prediction2 =
            objects.prediction {
                arrivalTime = now + 2.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.braintreeSouth)
                stopId = RedLine.jfkUmass.south2.id
            }
        val prediction3 =
            objects.prediction {
                arrivalTime = now + 9.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }
        val prediction4 =
            objects.prediction {
                arrivalTime = now + 15.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.braintreeSouth)
                stopId = RedLine.jfkUmass.south2.id
            }

        assertEquals(
            LeafFormat.branched {
                branch(
                    "Ashmont",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Approaching
                        ),
                        null
                    )
                )
                branch(
                    "Braintree",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(2)
                        ),
                        null
                    )
                )
                branch(
                    "Ashmont",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction3),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(9)
                        ),
                        null
                    )
                )
            },
            RouteCardData.Leaf(
                    0,
                    listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                    setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3),
                        objects.upcomingTrip(prediction4)
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(
                    now,
                    RedLine.route,
                    RedLine.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    @Test
    fun `formats Red Line northbound as non-branching showing next 2 trips`() = parametricTest {
        val objects = RedLine.objects()
        val now = Clock.System.now()
        val prediction1 =
            objects.prediction {
                arrivalTime = now + 3.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontNorth)
                stopId = RedLine.jfkUmass.north1.id
            }
        val prediction2 =
            objects.prediction {
                arrivalTime = now + 12.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.braintreeNorth)
                stopId = RedLine.jfkUmass.north2.id
            }
        val prediction3 =
            objects.prediction {
                arrivalTime = now + 18.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontNorth)
                stopId = RedLine.jfkUmass.north1.id
            }

        assertEquals(
            LeafFormat.Single(
                "Alewife",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(3)
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(12)
                        ),
                    ),
                    null
                )
            ),
            RouteCardData.Leaf(
                    1,
                    listOf(RedLine.ashmontNorth, RedLine.braintreeNorth),
                    setOf(RedLine.jfkUmass.north1.id, RedLine.jfkUmass.north2.id),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3)
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(
                    now,
                    RedLine.route,
                    RedLine.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    @Test
    fun `formats Red Line southbound as branching even if next trips all match`() = parametricTest {
        val objects = RedLine.objects()
        val now = Clock.System.now()
        val prediction1 =
            objects.prediction {
                arrivalTime = now + 2.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }
        val prediction2 =
            objects.prediction {
                arrivalTime = now + 5.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }
        val prediction3 =
            objects.prediction {
                arrivalTime = now + 9.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }

        assertEquals(
            LeafFormat.branched {
                branch(
                    "Ashmont",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(2)
                        ),
                        null
                    )
                )
                branch(
                    "Ashmont",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(5)
                        ),
                        null
                    )
                )
                branch(
                    "Ashmont",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction3),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(9)
                        ),
                        null
                    )
                )
            },
            RouteCardData.Leaf(
                    0,
                    listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                    setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3)
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(now, RedLine.route, RedLine.global, anyEnumValue())
        )
    }

    @Test
    fun `formats Red Line southbound as non-branching if one branch is closed`() = parametricTest {
        // ⚠ Southbound to Ashmont 3 min 12 min
        val objects = RedLine.objects()
        val now = Clock.System.now()

        val northQuincy = objects.stop()
        val wollaston = objects.stop()
        val quincyCenter = objects.stop()
        val quincyAdams = objects.stop()
        val braintree = objects.stop()

        val stopsBraintreeBranch =
            listOf(
                RedLine.jfkUmass.south2,
                northQuincy,
                wollaston,
                quincyCenter,
                quincyAdams,
                braintree
            )
        val prediction1 =
            objects.prediction {
                arrivalTime = now + 3.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }

        val prediction2 =
            objects.prediction {
                arrivalTime = now + 12.minutes
                departureTime = arrivalTime
                trip = objects.trip(RedLine.ashmontSouth)
                stopId = RedLine.jfkUmass.south1.id
            }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity =
                    stopsBraintreeBranch
                        .map {
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = "Red",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = it.id,
                                trip = null
                            )
                        }
                        .toMutableList()
            }

        val mapStopRoute = MapStopRoute.matching(RedLine.route)

        assertEquals(
            LeafFormat.Single(
                "Ashmont",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(3)
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(12)
                        )
                    ),
                    UpcomingFormat.SecondaryAlert(StopAlertState.Issue, mapStopRoute)
                )
            ),
            RouteCardData.Leaf(
                    0,
                    listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                    setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                    listOf(objects.upcomingTrip(prediction1), objects.upcomingTrip(prediction2)),
                    listOf(alert),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList(),
                )
                .format(
                    now,
                    RedLine.route,
                    RedLine.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    @Test
    @Ignore // TODO once alerts are added
    fun `formats Red Line southbound as branching if one branch is disrupted`() = parametricTest {
        TODO("Southbound to Ashmont 1 min ⚠ Wollaston 2 min Ashmont 9 min")
    }

    private object GreenLine {
        private val objects = ObjectCollectionBuilder()

        fun objects() = objects.clone()

        val line = objects.line { id = "line-Green" }

        val b =
            objects.route {
                type = RouteType.LIGHT_RAIL
                lineId = line.id
            }
        val c =
            objects.route {
                type = RouteType.LIGHT_RAIL
                lineId = line.id
            }
        val d =
            objects.route {
                type = RouteType.LIGHT_RAIL
                lineId = line.id
            }
        val e =
            objects.route {
                type = RouteType.LIGHT_RAIL
                lineId = line.id
            }

        val bWestbound =
            objects.routePattern(b) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Boston College" }
            }
        val cWestbound =
            objects.routePattern(c) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Cleveland Circle" }
            }
        val dWestbound =
            objects.routePattern(d) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Riverside" }
            }
        val eWestbound =
            objects.routePattern(e) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Heath Street" }
            }

        val global = GlobalResponse(objects)
    }

    @Test
    fun `formats Green Line westbound at Boylston as branching`() = parametricTest {
        val objects = GreenLine.objects()
        val now = Clock.System.now()

        val prediction1 =
            objects.prediction {
                departureTime = now + 3.minutes
                trip = objects.trip(GreenLine.cWestbound)
            }
        val prediction2 =
            objects.prediction {
                departureTime = now + 5.minutes
                trip = objects.trip(GreenLine.bWestbound)
            }
        val prediction3 =
            objects.prediction {
                departureTime = now + 10.minutes
                trip = objects.trip(GreenLine.dWestbound)
            }
        val prediction4 =
            objects.prediction {
                departureTime = now + 15.minutes
                trip = objects.trip(GreenLine.eWestbound)
            }

        assertEquals(
            LeafFormat.branched {
                branch(
                    GreenLine.c,
                    "Cleveland Circle",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(3)
                        ),
                        null
                    )
                )
                branch(
                    GreenLine.b,
                    "Boston College",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5)
                        ),
                        null
                    )
                )
                branch(
                    GreenLine.d,
                    "Riverside",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction3),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(10)
                        ),
                        null
                    )
                )
            },
            RouteCardData.Leaf(
                    0,
                    listOf(
                        GreenLine.bWestbound,
                        GreenLine.cWestbound,
                        GreenLine.dWestbound,
                        GreenLine.eWestbound
                    ),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3),
                        objects.upcomingTrip(prediction4)
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(
                    now,
                    GreenLine.b,
                    GreenLine.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    @Test
    @Ignore // TODO once downstream alerts are added
    fun `formats Green Line westbound at Boylston as downstream disrupted`() = parametricTest {
        TODO("Westbound to C Cleveland Circle 3 min B⚠ Boston College 5 min D Riverside 10 min")
    }

    private object ProvidenceStoughtonLine {
        private val objects = ObjectCollectionBuilder()

        fun objects() = objects.clone()

        val route = objects.route { type = RouteType.COMMUTER_RAIL }

        val toWickford =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Wickford Junction" }
            }
        val toStoughton =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Stoughton" }
            }
        val toProvidence =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Providence" }
            }
        val fromWickford =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "South Station" }
            }
        val fromStoughton =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "South Station" }
            }
        val fromProvidence =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "South Station" }
            }

        val global = GlobalResponse(objects)
    }

    @Test
    fun `formats Providence Stoughton Line outbound at Ruggles as branching`() = parametricTest {
        val objects = ProvidenceStoughtonLine.objects()
        val today = Clock.System.now().toBostonTime().date
        val thisTime = LocalTime(hour = 10, minute = 0)
        val now = today.atTime(thisTime).fromBostonTime()

        val prediction1 =
            objects.prediction {
                departureTime = today.atTime(hour = 12, minute = 5).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.toStoughton)
            }
        val schedule2 =
            objects.schedule {
                departureTime = today.atTime(hour = 12 + 3, minute = 28).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.toProvidence)
            }
        val schedule3 =
            objects.schedule {
                departureTime = today.atTime(hour = 12 + 4, minute = 1).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.toWickford)
            }

        assertEquals(
            LeafFormat.branched {
                branch(
                    "Stoughton",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.Time(prediction1.departureTime!!, true)
                        ),
                        null
                    )
                )
                branch(
                    "Providence",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(schedule2),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.ScheduleTime(schedule2.departureTime!!, true)
                        ),
                        null
                    )
                )
                branch(
                    "Wickford Junction",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(schedule3),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.ScheduleTime(schedule3.departureTime!!, true)
                        ),
                        null
                    )
                )
            },
            RouteCardData.Leaf(
                    0,
                    listOf(
                        ProvidenceStoughtonLine.toProvidence,
                        ProvidenceStoughtonLine.toStoughton,
                        ProvidenceStoughtonLine.toWickford
                    ),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(schedule2),
                        objects.upcomingTrip(schedule3)
                    ),
                    emptyList(),
                    true,
                    true,
                    emptyList()
                )
                .format(
                    now,
                    ProvidenceStoughtonLine.route,
                    ProvidenceStoughtonLine.global,
                    anyEnumValue()
                )
        )
    }

    @Test
    fun `formats Providence Stoughton Line inbound at Ruggles as non-branching`() = parametricTest {
        val objects = ProvidenceStoughtonLine.objects()
        val today = Clock.System.now().toBostonTime().date
        val thisTime = LocalTime(hour = 10, minute = 0)
        val now = today.atTime(thisTime).fromBostonTime()

        val prediction1 =
            objects.prediction {
                departureTime = today.atTime(hour = 12 + 3, minute = 31).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.fromStoughton)
            }
        val prediction2 =
            objects.prediction {
                departureTime = today.atTime(hour = 12 + 3, minute = 53).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.fromProvidence)
            }
        val schedule3 =
            objects.schedule {
                departureTime = today.atTime(hour = 12 + 4, minute = 14).fromBostonTime()
                trip = objects.trip(ProvidenceStoughtonLine.fromWickford)
            }

        assertEquals(
            LeafFormat.Single(
                "South Station",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.Time(prediction1.departureTime!!, true)
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.Time(prediction2.departureTime!!, true)
                        )
                    ),
                    null
                )
            ),
            RouteCardData.Leaf(
                    1,
                    listOf(
                        ProvidenceStoughtonLine.fromProvidence,
                        ProvidenceStoughtonLine.fromStoughton,
                        ProvidenceStoughtonLine.fromWickford
                    ),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(schedule3)
                    ),
                    emptyList(),
                    true,
                    true,
                    emptyList()
                )
                .format(
                    now,
                    ProvidenceStoughtonLine.route,
                    ProvidenceStoughtonLine.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    private object `87` {
        private val objects = ObjectCollectionBuilder()

        fun objects() = objects.clone()

        val route =
            objects.route {
                type = RouteType.BUS
                directionNames = listOf("Outbound", "Inbound")
                directionDestinations =
                    listOf("Clarendon Hill or Arlington Center", "Lechmere Station")
            }
        val outboundTypical =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Arlington Center" }
            }
        val outboundDeviation =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Clarendon Hill" }
            }

        val global = GlobalResponse(objects)
    }

    @Test
    fun `formats bus as non-branching if next two trips match`() = parametricTest {
        val objects = `87`.objects()
        val now = Clock.System.now()

        val prediction1 =
            objects.prediction {
                departureTime = now + 3.minutes
                trip = objects.trip(`87`.outboundTypical)
            }
        val prediction2 =
            objects.prediction {
                departureTime = now + 12.minutes
                trip = objects.trip(`87`.outboundTypical)
            }
        val prediction3 =
            objects.prediction {
                departureTime = now + 35.minutes
                trip = objects.trip(`87`.outboundDeviation)
            }

        assertEquals(
            LeafFormat.Single(
                "Arlington Center",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(3)
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(12)
                        ),
                    ),
                    null
                )
            ),
            RouteCardData.Leaf(
                    0,
                    listOf(`87`.outboundTypical, `87`.outboundDeviation),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3)
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(
                    now,
                    `87`.route,
                    `87`.global,
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered)
                )
        )
    }

    @Test
    fun `formats bus as branching if next two trips differ`() = parametricTest {
        val objects = `87`.objects()
        val now = Clock.System.now()

        val prediction1 =
            objects.prediction {
                departureTime = now + 1.minutes
                trip = objects.trip(`87`.outboundTypical)
            }
        val prediction2 =
            objects.prediction {
                departureTime = now + 32.minutes
                trip = objects.trip(`87`.outboundDeviation)
            }

        assertEquals(
            LeafFormat.branched {
                branch(
                    "Arlington Center",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(1)
                        ),
                        null
                    )
                )
                branch(
                    "Clarendon Hill",
                    UpcomingFormat.Some(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(32)
                        ),
                        null
                    )
                )
            },
            RouteCardData.Leaf(
                    0,
                    listOf(`87`.outboundTypical, `87`.outboundDeviation),
                    emptySet(),
                    listOf(objects.upcomingTrip(prediction1), objects.upcomingTrip(prediction2)),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList()
                )
                .format(now, `87`.route, `87`.global, anyEnumValue())
        )
    }

    @Test
    fun `formats Red Line southbound as branching if service ended on one branch`() =
        parametricTest {
            val objects = RedLine.objects()
            val now = Clock.System.now()

            val prediction1 =
                objects.prediction {
                    departureTime = now + 1.minutes
                    trip = objects.trip(RedLine.ashmontSouth)
                }
            val prediction2 =
                objects.prediction {
                    departureTime = now + 2.minutes
                    trip = objects.trip(RedLine.ashmontSouth)
                }

            assertEquals(
                LeafFormat.branched {
                    branch(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Approaching
                            ),
                            null
                        )
                    )
                    branch(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(2)
                            ),
                            null
                        )
                    )
                },
                RouteCardData.Leaf(
                        0,
                        listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2)
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList()
                    )
                    .format(now, RedLine.route, RedLine.global, anyEnumValue())
            )
        }

    @Test
    fun `formats Red Line southbound as non-branching if service ended on all branches`() =
        parametricTest {
            val now = Clock.System.now()

            assertEquals(
                LeafFormat.Single(
                    null,
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday)
                ),
                RouteCardData.Leaf(
                        0,
                        listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                        emptySet(),
                        emptyList(),
                        emptyList(),
                        true,
                        true,
                        emptyList()
                    )
                    .format(now, RedLine.route, RedLine.global, anyEnumValue())
            )
        }

    @Test
    fun `formats Green Line westbound at Boylston as branching hiding no trips if predictions unavailable on one branch`() =
        parametricTest {
            val objects = GreenLine.objects()
            val now = Clock.System.now()

            val prediction1 =
                objects.prediction {
                    departureTime = now + 3.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                }
            val prediction2 =
                objects.prediction {
                    departureTime = now + 5.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                }
            val prediction3 =
                objects.prediction {
                    departureTime = now + 10.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                }

            assertEquals(
                LeafFormat.branched {
                    branch(
                        GreenLine.c,
                        "Cleveland Circle",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(3)
                            ),
                            null
                        )
                    )
                    branch(
                        GreenLine.b,
                        "Boston College",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(5)
                            ),
                            null
                        )
                    )
                    branch(
                        GreenLine.b,
                        "Boston College",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction3),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(10)
                            ),
                            null
                        )
                    )
                },
                RouteCardData.Leaf(
                        0,
                        listOf(
                            GreenLine.bWestbound,
                            GreenLine.cWestbound,
                            GreenLine.dWestbound,
                            GreenLine.eWestbound
                        ),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3)
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList()
                    )
                    .format(now, GreenLine.b, GreenLine.global, anyEnumValue())
            )
        }

    @Test
    fun `formats Green Line westbound at Boylston as non-branching showing no trips if predictions unavailable on all branches`() =
        parametricTest {
            val objects = ObjectCollectionBuilder()
            val now = Clock.System.now()

            val schedule1 =
                objects.schedule {
                    departureTime = now + 15.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                }
            val schedule2 =
                objects.schedule {
                    departureTime = now + 15.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                }

            assertEquals(
                LeafFormat.Single(
                    null,
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable)
                ),
                RouteCardData.Leaf(
                        0,
                        listOf(
                            GreenLine.bWestbound,
                            GreenLine.cWestbound,
                            GreenLine.dWestbound,
                            GreenLine.eWestbound
                        ),
                        emptySet(),
                        listOf(objects.upcomingTrip(schedule1), objects.upcomingTrip(schedule2)),
                        emptyList(),
                        true,
                        true,
                        emptyList()
                    )
                    .format(now, GreenLine.b, GreenLine.global, anyEnumValue())
            )
        }

    @Test
    @Ignore // TODO once alerts are added
    fun `formats Green Line westbound at Kenmore as branching showing alert if disruption on one branch`() =
        parametricTest {
            TODO(
                "Westbound to C Cleveland Circle 3 min B Boston College 5 min D Riverside Shuttle Bus"
            )
        }

    @Test
    @Ignore // TODO once alerts are added
    fun `formats Green Line westbound at Boylston as branching showing no trips if disruption and predictions unavailable`() =
        parametricTest {
            TODO(
                "Westbound to B Boston College Predictions unavailable C Cleveland Circle Predictions unavailable D Riverside Shuttle Bus"
            )
        }

    @Test
    @Ignore // TODO once alerts are added
    fun `formats Green Line westbound at Boylston as non-branching if disruption on all branches`() =
        parametricTest {
            TODO("Westbound Shuttle Bus")
        }
}
