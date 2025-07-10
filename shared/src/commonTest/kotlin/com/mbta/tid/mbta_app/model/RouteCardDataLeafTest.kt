package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.utils.fromBostonTime
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime

class RouteCardDataLeafTest {
    private fun ParametricTest.anyNonScheduleBasedRouteType() =
        anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY)

    /**
     * Helper function to get rid of auto generated UUIDs in the branch format IDs in generated
     * format objects. These are required for SwiftUI ForEach views, but when we're generating
     * separate expected and actual objects in tests, they get in the way.
     */
    private fun wipeBranchUUID(format: LeafFormat): LeafFormat {
        return when (format) {
            is LeafFormat.Single -> format
            is LeafFormat.Branched ->
                format.copy(
                    format.branchRows.map {
                        it.copy(id = it.id.split("-").subList(0, 2).joinToString("-"))
                    }
                )
        }
    }

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
            LeafFormat.Single(
                route = null,
                headsign = null,
                UpcomingFormat.Disruption(alert, "alert-large-red-suspension"),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    listOf(alert),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                objects.route { type = RouteType.HEAVY_RAIL } to "alert-borderless-issue",
            )

        val alert = objects.alert { effect = Alert.Effect.ServiceChange }

        val context: RouteCardData.Context = anyEnumValue()
        for ((route, icon) in cases) {
            assertEquals(
                LeafFormat.Single(
                    route = null,
                    headsign = null,
                    UpcomingFormat.NoTrips(
                        UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                        UpcomingFormat.SecondaryAlert(icon),
                    ),
                ),
                RouteCardData.Leaf(
                        RouteCardData.LineOrRoute.Route(route),
                        objects.stop(),
                        0,
                        emptyList(),
                        emptySet(),
                        emptyList(),
                        listOf(alert),
                        true,
                        true,
                        emptyList(),
                        context,
                    )
                    .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.Disruption(alert, "alert-large-silver-suspension"),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip,
                            route.type,
                            TripInstantDisplay.Minutes(1),
                        )
                    ),
                    UpcomingFormat.SecondaryAlert("alert-large-bus-issue"),
                ),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    listOf(alert),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip,
                            route.type,
                            TripInstantDisplay.Minutes(1),
                        )
                    ),
                    UpcomingFormat.SecondaryAlert("alert-large-bus-issue"),
                ),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip),
                    emptyList(),
                    true,
                    true,
                    listOf(alert),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyNonScheduleBasedRouteType() }

        assertEquals(
            LeafFormat.Single(
                route = null,
                headsign = null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    emptyList(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(objects.upcomingTrip(schedule)),
                    emptyList(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
        )
    }

    @Test
    fun `formats as loading if empty trips but still loading`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }
        val pattern = objects.routePattern(route)
        assertEquals(
            LeafFormat.Single(route = null, headsign = null, UpcomingFormat.Loading),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    listOf(pattern),
                    emptySet(),
                    emptyList(),
                    emptyList(),
                    false,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip2,
                            route.type,
                            TripInstantDisplay.Minutes(5),
                        )
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
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
                route = null,
                headsign = "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip2,
                            subwayRoute.type,
                            TripInstantDisplay.Minutes(5),
                        )
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(subwayRoute),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
        )
        assertEquals(
            LeafFormat.Single(
                route = null,
                headsign = "",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip1,
                            busRoute.type,
                            TripInstantDisplay.ScheduleMinutes(5),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            upcomingTrip2,
                            busRoute.type,
                            TripInstantDisplay.Minutes(5),
                        ),
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(busRoute),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    listOf(upcomingTrip1, upcomingTrip2),
                    listOf(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
        )
    }

    @Test
    fun `format handles no schedules all day`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }

        assertEquals(
            LeafFormat.Single(
                route = null,
                headsign = null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday),
            ),
            RouteCardData.Leaf(
                    RouteCardData.LineOrRoute.Route(route),
                    objects.stop(),
                    0,
                    emptyList(),
                    emptySet(),
                    emptyList(),
                    listOf(),
                    true,
                    false,
                    emptyList(),
                    anyEnumValue(),
                )
                .format(now, GlobalResponse(objects)),
        )
    }

    private object RedLine {
        private val objects = TestData.clone()

        fun objects() = objects.clone()

        val route = objects.getRoute("Red")
        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)

        object jfkUmass {
            val itself = objects.getStop("place-jfk")
            val south1 = objects.getStop("70085")
            val south2 = objects.getStop("70095")
            val north1 = objects.getStop("70086")
            val north2 = objects.getStop("70096")
        }

        val stopsBraintreeBranchSouth = listOf("70095", "70097", "70099", "70101", "70103", "70103")

        val ashmontSouth = objects.getRoutePattern("Red-1-0")
        val braintreeSouth = objects.getRoutePattern("Red-3-0")
        val ashmontNorth = objects.getRoutePattern("Red-1-1")
        val braintreeNorth = objects.getRoutePattern("Red-3-1")

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
            wipeBranchUUID(
                LeafFormat.branched {
                    branchRow(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Approaching,
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Braintree",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(2),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction3),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(9),
                            ),
                            null,
                        ),
                    )
                }
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        RedLine.lineOrRoute,
                        RedLine.jfkUmass.itself,
                        0,
                        listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                        setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3),
                            objects.upcomingTrip(prediction4),
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList(),
                        anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                    )
                    .format(now, RedLine.global)
            ),
        )
    }

    @Test
    fun `formats Red Line southbound as branching showing next 3 trips with one secondary alert`() =
        parametricTest {
            val objects = RedLine.objects()
            val now = Clock.System.now()

            val downstreamAlert = objects.alert { effect = Alert.Effect.Shuttle }
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
                wipeBranchUUID(
                    LeafFormat.branched {
                        secondaryAlert =
                            UpcomingFormat.SecondaryAlert(StopAlertState.Issue, MapStopRoute.RED)
                        branchRow(
                            "Ashmont",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction1),
                                    RouteType.HEAVY_RAIL,
                                    TripInstantDisplay.Approaching,
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            "Braintree",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction2),
                                    RouteType.HEAVY_RAIL,
                                    TripInstantDisplay.Minutes(2),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            "Ashmont",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction3),
                                    RouteType.HEAVY_RAIL,
                                    TripInstantDisplay.Minutes(9),
                                ),
                                null,
                            ),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            RedLine.lineOrRoute,
                            RedLine.jfkUmass.itself,
                            0,
                            listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                            setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                                objects.upcomingTrip(prediction3),
                                objects.upcomingTrip(prediction4),
                            ),
                            emptyList(),
                            allDataLoaded = true,
                            hasSchedulesToday = true,
                            listOf(downstreamAlert),
                            anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                        )
                        .format(now, RedLine.global)
                ),
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
                route = null,
                headsign = "Alewife",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(3),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.HEAVY_RAIL,
                            TripInstantDisplay.Minutes(12),
                        ),
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    RedLine.lineOrRoute,
                    RedLine.jfkUmass.itself,
                    1,
                    listOf(RedLine.ashmontNorth, RedLine.braintreeNorth),
                    setOf(RedLine.jfkUmass.north1.id, RedLine.jfkUmass.north2.id),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3),
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList(),
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                )
                .format(now, RedLine.global),
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
            wipeBranchUUID(
                LeafFormat.branched {
                    branchRow(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(2),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(5),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Ashmont",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction3),
                                RouteType.HEAVY_RAIL,
                                TripInstantDisplay.Minutes(9),
                            ),
                            null,
                        ),
                    )
                }
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        RedLine.lineOrRoute,
                        RedLine.jfkUmass.itself,
                        0,
                        listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                        setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3),
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList(),
                        anyEnumValue(),
                    )
                    .format(now, RedLine.global)
            ),
        )
    }

    @Test
    fun `formats Red Line southbound as branching with Braintree suspended from JFK`() =
        parametricTest {
            // ⚠ Southbound to Ashmont 3 min 12 min
            val objects = RedLine.objects()
            val now = Clock.System.now()

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
                        RedLine.stopsBraintreeBranchSouth
                            .map {
                                Alert.InformedEntity(
                                    activities = listOf(Alert.InformedEntity.Activity.Board),
                                    directionId = 0,
                                    route = RedLine.route.id,
                                    routeType = RouteType.HEAVY_RAIL,
                                    stop = it,
                                    trip = null,
                                )
                            }
                            .toMutableList()
                }

            val mapStopRoute = MapStopRoute.matching(RedLine.route)

            assertEquals(
                wipeBranchUUID(
                    LeafFormat.Branched(
                        listOf(
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Ashmont",
                                UpcomingFormat.Some(
                                    UpcomingFormat.Some.FormattedTrip(
                                        objects.upcomingTrip(prediction1),
                                        RouteType.HEAVY_RAIL,
                                        TripInstantDisplay.Minutes(3),
                                    ),
                                    null,
                                ),
                            ),
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Ashmont",
                                UpcomingFormat.Some(
                                    UpcomingFormat.Some.FormattedTrip(
                                        objects.upcomingTrip(prediction2),
                                        RouteType.HEAVY_RAIL,
                                        TripInstantDisplay.Minutes(12),
                                    ),
                                    null,
                                ),
                            ),
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Braintree",
                                UpcomingFormat.Disruption(alert, mapStopRoute),
                            ),
                        )
                    )
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            RedLine.lineOrRoute,
                            RedLine.jfkUmass.itself,
                            0,
                            listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                            setOf(RedLine.jfkUmass.south1.id, RedLine.jfkUmass.south2.id),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                            ),
                            listOf(alert),
                            allDataLoaded = true,
                            hasSchedulesToday = true,
                            emptyList(),
                            anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                        )
                        .format(now, RedLine.global)
                ),
            )
        }

    @Test
    @Ignore // TODO once alerts are added
    fun `formats Red Line southbound as branching if one branch is disrupted downstream`() =
        parametricTest {
            TODO("Southbound to Ashmont 1 min ⚠ Wollaston 2 min Ashmont 9 min")
        }

    private object GreenLine {
        private val objects = TestData.clone()

        fun objects() = objects.clone()

        val line = objects.getLine("line-Green")

        val b = objects.getRoute("Green-B")
        val c = objects.getRoute("Green-C")
        val d = objects.getRoute("Green-D")
        val e = objects.getRoute("Green-E")

        val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(b, c, d, e))

        val boylston = objects.getStop("place-boyls")
        val kenmore = objects.getStop("place-kencl")
        val reservoir = objects.getStop("place-rsmnl")

        val bWestbound = objects.getRoutePattern("Green-B-812-0")
        val cWestbound = objects.getRoutePattern("Green-C-832-0")
        val dWestbound = objects.getRoutePattern("Green-D-855-0")
        val eWestbound = objects.getRoutePattern("Green-E-886-0")

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
            wipeBranchUUID(
                LeafFormat.branched {
                    branchRow(
                        GreenLine.c,
                        "Cleveland Circle",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(3),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        GreenLine.b,
                        "Boston College",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(5),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        GreenLine.d,
                        "Riverside",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction3),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(10),
                            ),
                            null,
                        ),
                    )
                }
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        GreenLine.lineOrRoute,
                        GreenLine.boylston,
                        0,
                        listOf(
                            GreenLine.bWestbound,
                            GreenLine.cWestbound,
                            GreenLine.dWestbound,
                            GreenLine.eWestbound,
                        ),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3),
                            objects.upcomingTrip(prediction4),
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList(),
                        anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                    )
                    .format(now, GreenLine.global)
            ),
        )
    }

    @Test
    fun `formats Green Line westbound at Reservoir as single`() = parametricTest {
        val objects = GreenLine.objects()
        val now = Clock.System.now()

        val prediction1 =
            objects.prediction {
                departureTime = now + 3.minutes
                trip = objects.trip(GreenLine.dWestbound)
            }
        val prediction2 =
            objects.prediction {
                departureTime = now + 5.minutes
                trip = objects.trip(GreenLine.dWestbound)
            }
        val prediction3 =
            objects.prediction {
                departureTime = now + 10.minutes
                trip = objects.trip(GreenLine.dWestbound)
            }
        val prediction4 =
            objects.prediction {
                departureTime = now + 15.minutes
                trip = objects.trip(GreenLine.dWestbound)
            }

        assertEquals(
            LeafFormat.Single(
                GreenLine.d,
                "Riverside",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(3),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5),
                        ),
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    GreenLine.lineOrRoute,
                    GreenLine.reservoir,
                    0,
                    listOf(GreenLine.dWestbound),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3),
                        objects.upcomingTrip(prediction4),
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList(),
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                )
                .format(now, GreenLine.global),
        )
    }

    @Test
    @Ignore // TODO once downstream alerts are added
    fun `formats Green Line westbound at Boylston as downstream disrupted`() = parametricTest {
        TODO("Westbound to C Cleveland Circle 3 min B⚠ Boston College 5 min D Riverside 10 min")
    }

    private object ProvidenceStoughtonLine {
        private val objects = TestData.clone()

        fun objects() = objects.clone()

        val route = objects.getRoute("CR-Providence")

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)

        val toWickford = objects.getRoutePattern("CR-Providence-9cf54fb3-0")
        val toStoughton = objects.getRoutePattern("CR-Providence-9515a09b-0")
        val toProvidence =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Providence" }
            }
        val fromWickford = objects.getRoutePattern("CR-Providence-e9395acc-1")
        val fromStoughton = objects.getRoutePattern("CR-Providence-6cae46be-1")
        val fromProvidence =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "South Station" }
            }

        val ruggles = objects.getStop("place-rugg")

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
            wipeBranchUUID(
                LeafFormat.branched {
                    branchRow(
                        "Stoughton",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.COMMUTER_RAIL,
                                TripInstantDisplay.Time(prediction1.departureTime!!, true),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Providence",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(schedule2),
                                RouteType.COMMUTER_RAIL,
                                TripInstantDisplay.ScheduleTime(schedule2.departureTime!!, true),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Wickford Junction",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(schedule3),
                                RouteType.COMMUTER_RAIL,
                                TripInstantDisplay.ScheduleTime(schedule3.departureTime!!, true),
                            ),
                            null,
                        ),
                    )
                }
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        ProvidenceStoughtonLine.lineOrRoute,
                        ProvidenceStoughtonLine.ruggles,
                        0,
                        listOf(
                            ProvidenceStoughtonLine.toProvidence,
                            ProvidenceStoughtonLine.toStoughton,
                            ProvidenceStoughtonLine.toWickford,
                        ),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(schedule2),
                            objects.upcomingTrip(schedule3),
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        anyEnumValue(),
                    )
                    .format(now, ProvidenceStoughtonLine.global)
            ),
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
                route = null,
                headsign = "South Station",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.Time(prediction1.departureTime!!, true),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.COMMUTER_RAIL,
                            TripInstantDisplay.Time(prediction2.departureTime!!, true),
                        ),
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    ProvidenceStoughtonLine.lineOrRoute,
                    ProvidenceStoughtonLine.ruggles,
                    1,
                    listOf(
                        ProvidenceStoughtonLine.fromProvidence,
                        ProvidenceStoughtonLine.fromStoughton,
                        ProvidenceStoughtonLine.fromWickford,
                    ),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(schedule3),
                    ),
                    emptyList(),
                    true,
                    true,
                    emptyList(),
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                )
                .format(now, ProvidenceStoughtonLine.global),
        )
    }

    private object `87` {
        private val objects = TestData.clone()

        fun objects() = objects.clone()

        val route = objects.getRoute("87")
        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
        val outboundTypical = objects.getRoutePattern("87-2-0")
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
                route = null,
                headsign = "Arlington Center",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction1),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(3),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            objects.upcomingTrip(prediction2),
                            RouteType.BUS,
                            TripInstantDisplay.Minutes(12),
                        ),
                    ),
                    null,
                ),
            ),
            RouteCardData.Leaf(
                    `87`.lineOrRoute,
                    objects.stop(),
                    0,
                    listOf(`87`.outboundTypical, `87`.outboundDeviation),
                    emptySet(),
                    listOf(
                        objects.upcomingTrip(prediction1),
                        objects.upcomingTrip(prediction2),
                        objects.upcomingTrip(prediction3),
                    ),
                    emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = true,
                    emptyList(),
                    anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                )
                .format(now, `87`.global),
        )
    }

    @Test
    fun `formats bus as non-branching in StopDetailsFiltered even if next two trips match`() {
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
            wipeBranchUUID(
                LeafFormat.Branched(
                    branchRows =
                        listOf(
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Arlington Center",
                                UpcomingFormat.Some(
                                    UpcomingFormat.Some.FormattedTrip(
                                        objects.upcomingTrip(prediction1),
                                        RouteType.BUS,
                                        TripInstantDisplay.Minutes(3),
                                    ),
                                    null,
                                ),
                            ),
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Arlington Center",
                                UpcomingFormat.Some(
                                    UpcomingFormat.Some.FormattedTrip(
                                        objects.upcomingTrip(prediction2),
                                        RouteType.BUS,
                                        TripInstantDisplay.Minutes(12),
                                    ),
                                    null,
                                ),
                            ),
                            LeafFormat.Branched.BranchRow(
                                null,
                                "Clarendon Hill",
                                UpcomingFormat.Some(
                                    UpcomingFormat.Some.FormattedTrip(
                                        objects.upcomingTrip(prediction3),
                                        RouteType.BUS,
                                        TripInstantDisplay.Minutes(35),
                                    ),
                                    null,
                                ),
                            ),
                        ),
                    null,
                )
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        `87`.lineOrRoute,
                        objects.stop(),
                        0,
                        listOf(`87`.outboundTypical, `87`.outboundDeviation),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3),
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList(),
                        RouteCardData.Context.StopDetailsFiltered,
                    )
                    .format(now, `87`.global)
            ),
        )
    }

    @Test
    fun `formats bus as branching if next trips differ and only shows 2 trips`() = parametricTest {
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

        val prediction3 =
            objects.prediction {
                departureTime = now + 60.minutes
                trip = objects.trip(`87`.outboundTypical)
            }

        assertEquals(
            wipeBranchUUID(
                LeafFormat.branched {
                    branchRow(
                        "Arlington Center",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction1),
                                RouteType.BUS,
                                TripInstantDisplay.Minutes(1),
                            ),
                            null,
                        ),
                    )
                    branchRow(
                        "Clarendon Hill",
                        UpcomingFormat.Some(
                            UpcomingFormat.Some.FormattedTrip(
                                objects.upcomingTrip(prediction2),
                                RouteType.BUS,
                                TripInstantDisplay.Minutes(32),
                            ),
                            null,
                        ),
                    )
                }
            ),
            wipeBranchUUID(
                RouteCardData.Leaf(
                        `87`.lineOrRoute,
                        objects.stop(),
                        0,
                        listOf(`87`.outboundTypical, `87`.outboundDeviation),
                        emptySet(),
                        listOf(
                            objects.upcomingTrip(prediction1),
                            objects.upcomingTrip(prediction2),
                            objects.upcomingTrip(prediction3),
                        ),
                        emptyList(),
                        allDataLoaded = true,
                        hasSchedulesToday = true,
                        emptyList(),
                        anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                    )
                    .format(now, `87`.global)
            ),
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
                wipeBranchUUID(
                    LeafFormat.branched {
                        branchRow(
                            "Ashmont",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction1),
                                    RouteType.HEAVY_RAIL,
                                    TripInstantDisplay.Approaching,
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            "Ashmont",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction2),
                                    RouteType.HEAVY_RAIL,
                                    TripInstantDisplay.Minutes(2),
                                ),
                                null,
                            ),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            RedLine.lineOrRoute,
                            objects.stop(),
                            0,
                            listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                            emptySet(),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                            ),
                            emptyList(),
                            allDataLoaded = true,
                            hasSchedulesToday = true,
                            emptyList(),
                            anyEnumValue(),
                        )
                        .format(now, RedLine.global)
                ),
            )
        }

    @Test
    fun `formats Red Line southbound as non-branching if service ended on all branches`() =
        parametricTest {
            val objects = RedLine.objects()
            val now = Clock.System.now()

            assertEquals(
                LeafFormat.Single(
                    route = null,
                    headsign = null,
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday),
                ),
                RouteCardData.Leaf(
                        RedLine.lineOrRoute,
                        objects.stop(),
                        0,
                        listOf(RedLine.ashmontSouth, RedLine.braintreeSouth),
                        emptySet(),
                        emptyList(),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        anyEnumValue(),
                    )
                    .format(now, RedLine.global),
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
                wipeBranchUUID(
                    LeafFormat.branched {
                        branchRow(
                            GreenLine.c,
                            "Cleveland Circle",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction1),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(3),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.b,
                            "Boston College",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction2),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(5),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.b,
                            "Boston College",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction3),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(10),
                                ),
                                null,
                            ),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            GreenLine.lineOrRoute,
                            GreenLine.boylston,
                            0,
                            listOf(
                                GreenLine.bWestbound,
                                GreenLine.cWestbound,
                                GreenLine.dWestbound,
                                GreenLine.eWestbound,
                            ),
                            emptySet(),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                                objects.upcomingTrip(prediction3),
                            ),
                            emptyList(),
                            true,
                            true,
                            emptyList(),
                            anyEnumValue(),
                        )
                        .format(now, GreenLine.global)
                ),
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
                    route = null,
                    headsign = null,
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
                ),
                RouteCardData.Leaf(
                        GreenLine.lineOrRoute,
                        GreenLine.boylston,
                        0,
                        listOf(
                            GreenLine.bWestbound,
                            GreenLine.cWestbound,
                            GreenLine.dWestbound,
                            GreenLine.eWestbound,
                        ),
                        emptySet(),
                        listOf(objects.upcomingTrip(schedule1), objects.upcomingTrip(schedule2)),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        anyEnumValue(),
                    )
                    .format(now, GreenLine.global),
            )
        }

    @Test
    fun `formats Green Line westbound at Kenmore as branching showing alert if disruption on one branch`() =
        parametricTest {
            val objects = GreenLine.objects()
            val now = Clock.System.now()

            val prediction1 =
                objects.prediction {
                    departureTime = now + 3.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                    stopId = GreenLine.kenmore.id
                }
            val prediction2 =
                objects.prediction {
                    departureTime = now + 5.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                    stopId = GreenLine.kenmore.id
                }
            val prediction3 = // Won't be shown - only 2 predictions shown + shuttle
                objects.prediction {
                    departureTime = now + 10.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                    stopId = GreenLine.kenmore.id
                }
            val alert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    informedEntity =
                        mutableListOf(
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.d.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.kenmore.id,
                                trip = null,
                            )
                        )
                }

            assertEquals(
                wipeBranchUUID(
                    LeafFormat.branched {
                        branchRow(
                            GreenLine.c,
                            "Cleveland Circle",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction1),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(3),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.b,
                            "Boston College",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction2),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(5),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.d,
                            "Riverside",
                            UpcomingFormat.Disruption(alert, MapStopRoute.matching(GreenLine.d)),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            GreenLine.lineOrRoute,
                            GreenLine.kenmore,
                            0,
                            listOf(
                                GreenLine.bWestbound,
                                GreenLine.cWestbound,
                                GreenLine.dWestbound,
                                GreenLine.eWestbound,
                            ),
                            setOf(GreenLine.kenmore.id),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                                objects.upcomingTrip(prediction3),
                            ),
                            listOf(alert),
                            true,
                            true,
                            emptyList(),
                            anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                        )
                        .format(now, GreenLine.global)
                ),
            )
        }

    @Test
    fun `formats Green Line westbound at Kenmore as branching showing alerts if disruption on 2 branches`() =
        parametricTest {
            val objects = GreenLine.objects()
            val now = Clock.System.now()

            val prediction1 =
                objects.prediction {
                    departureTime = now + 3.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                    stopId = GreenLine.kenmore.id
                }

            val prediction2 =
                objects.prediction {
                    departureTime = now + 5.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                    stopId = GreenLine.kenmore.id
                }
            val alert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    informedEntity =
                        mutableListOf(
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.b.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.kenmore.id,
                                trip = null,
                            ),
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.d.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.kenmore.id,
                                trip = null,
                            ),
                        )
                }

            assertEquals(
                wipeBranchUUID(
                    LeafFormat.branched {
                        branchRow(
                            GreenLine.c,
                            "Cleveland Circle",
                            UpcomingFormat.Some(
                                UpcomingFormat.Some.FormattedTrip(
                                    objects.upcomingTrip(prediction1),
                                    RouteType.LIGHT_RAIL,
                                    TripInstantDisplay.Minutes(3),
                                ),
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.b,
                            "Boston College",
                            UpcomingFormat.Disruption(alert, MapStopRoute.matching(GreenLine.b)),
                        )
                        branchRow(
                            GreenLine.d,
                            "Riverside",
                            UpcomingFormat.Disruption(alert, MapStopRoute.matching(GreenLine.d)),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            GreenLine.lineOrRoute,
                            GreenLine.kenmore,
                            0,
                            listOf(
                                GreenLine.bWestbound,
                                GreenLine.cWestbound,
                                GreenLine.dWestbound,
                                GreenLine.eWestbound,
                            ),
                            setOf(GreenLine.kenmore.id),
                            listOf(
                                objects.upcomingTrip(prediction1),
                                objects.upcomingTrip(prediction2),
                            ),
                            listOf(alert),
                            true,
                            true,
                            emptyList(),
                            anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                        )
                        .format(now, GreenLine.global)
                ),
            )
        }

    @Test
    fun `formats Green Line westbound at Boylston as branching showing no trips if disruption and predictions unavailable`() =
        parametricTest {
            val objects = GreenLine.objects()
            val now = Clock.System.now()

            val alert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    informedEntity =
                        mutableListOf(
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.d.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.boylston.id,
                                trip = null,
                            )
                        )
                }

            val schedB =
                objects.schedule {
                    departureTime = now + 3.minutes
                    trip = objects.trip(GreenLine.bWestbound)
                    stopId = GreenLine.boylston.id
                }

            val schedC =
                objects.schedule {
                    departureTime = now + 5.minutes
                    trip = objects.trip(GreenLine.cWestbound)
                    stopId = GreenLine.boylston.id
                }

            val schedD =
                objects.schedule {
                    departureTime = now + 7.minutes
                    trip = objects.trip(GreenLine.dWestbound)
                    stopId = GreenLine.boylston.id
                }

            val schedE =
                objects.schedule {
                    departureTime = now + 9.minutes
                    trip = objects.trip(GreenLine.dWestbound)
                    stopId = GreenLine.boylston.id
                }

            assertEquals(
                wipeBranchUUID(
                    LeafFormat.branched {
                        branchRow(
                            GreenLine.b,
                            "Boston College",
                            UpcomingFormat.NoTrips(
                                UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.c,
                            "Cleveland Circle",
                            UpcomingFormat.NoTrips(
                                UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                                null,
                            ),
                        )
                        branchRow(
                            GreenLine.d,
                            "Riverside",
                            UpcomingFormat.Disruption(alert, MapStopRoute.matching(GreenLine.d)),
                        )
                    }
                ),
                wipeBranchUUID(
                    RouteCardData.Leaf(
                            GreenLine.lineOrRoute,
                            GreenLine.boylston,
                            0,
                            listOf(
                                GreenLine.bWestbound,
                                GreenLine.cWestbound,
                                GreenLine.dWestbound,
                                GreenLine.eWestbound,
                            ),
                            setOf(GreenLine.boylston.id),
                            listOf(
                                objects.upcomingTrip(schedB),
                                objects.upcomingTrip(schedC),
                                objects.upcomingTrip(schedD),
                                objects.upcomingTrip(schedE),
                            ),
                            listOf(alert),
                            true,
                            mapOf(
                                GreenLine.bWestbound.id to true,
                                GreenLine.cWestbound.id to true,
                                GreenLine.dWestbound.id to true,
                                GreenLine.eWestbound.id to true,
                            ),
                            emptyList(),
                            anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                        )
                        .format(now, GreenLine.global)
                ),
            )
        }

    @Test
    fun `formats Green Line westbound at Boylston as non-branching if disruption on all branches`() =
        parametricTest {
            val objects = GreenLine.objects()
            val now = Clock.System.now()
            val alert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    informedEntity =
                        mutableListOf(
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.b.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.boylston.id,
                                trip = null,
                            ),
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.c.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.boylston.id,
                                trip = null,
                            ),
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.d.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.boylston.id,
                                trip = null,
                            ),
                            Alert.InformedEntity(
                                activities = listOf(Alert.InformedEntity.Activity.Board),
                                directionId = 0,
                                route = GreenLine.e.id,
                                routeType = RouteType.LIGHT_RAIL,
                                stop = GreenLine.boylston.id,
                                trip = null,
                            ),
                        )
                }

            assertEquals(
                LeafFormat.Single(
                    route = null,
                    headsign = null,
                    UpcomingFormat.Disruption(alert, MapStopRoute.matching(GreenLine.b)),
                ),
                RouteCardData.Leaf(
                        GreenLine.lineOrRoute,
                        GreenLine.boylston,
                        0,
                        listOf(
                            GreenLine.bWestbound,
                            GreenLine.cWestbound,
                            GreenLine.dWestbound,
                            GreenLine.eWestbound,
                        ),
                        setOf(GreenLine.boylston.id),
                        listOf(),
                        listOf(alert),
                        true,
                        true,
                        emptyList(),
                        anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                    )
                    .format(now, GreenLine.global),
            )
        }

    @Test
    fun `formats suspension alert as single when there is a stop_headsign for stop`() =
        parametricTest {
            val objects = ObjectCollectionBuilder()
            val route = objects.route { type = RouteType.FERRY }

            val stop1 = objects.stop {}
            val stop2 = objects.stop {}
            val stop3 = objects.stop {}

            val representativeTrip =
                objects.trip {
                    routeId = route.id
                    routePatternId = "rp1"
                    directionId = 0
                    stopIds = listOf(stop1.id, stop2.id)
                    headsign = "Quincy Loop"
                }

            val rp1 =
                objects.routePattern(route) {
                    id = "rp1"
                    typicality = RoutePattern.Typicality.Typical
                    representativeTripId = representativeTrip.id
                }

            // Pattern 2 isn't scheduled, but because typical will treat this as branched
            val representativeTrip2 =
                objects.trip {
                    routeId = route.id
                    routePatternId = "rp2"
                    directionId = 0
                    stopIds = listOf(stop1.id, stop3.id)
                    headsign = "Quincy Loop"
                }

            val rp2 =
                objects.routePattern(route) {
                    id = "rp2"
                    typicality = RoutePattern.Typicality.Typical
                    representativeTripId = representativeTrip.id
                }
            val now = Clock.System.now()
            val alert =
                objects.alert {
                    effect = Alert.Effect.Suspension
                    informedEntity =
                        mutableListOf(
                            Alert.InformedEntity(
                                activities =
                                    listOf(
                                        Alert.InformedEntity.Activity.Board,
                                        Alert.InformedEntity.Activity.Exit,
                                        Alert.InformedEntity.Activity.Ride,
                                    ),
                                directionId = 0,
                                route = route.id,
                                routeType = RouteType.FERRY,
                                stop = stop1.id,
                                trip = null,
                            )
                        )
                }

            val schedule =
                objects.schedule {
                    tripId = representativeTrip.id
                    stopId = stop1.id
                    departureTime = now + 5.minutes
                    stopHeadsign = "Central Wharf"
                }

            assertEquals(
                LeafFormat.Single(
                    route = null,
                    headsign = null,
                    UpcomingFormat.Disruption(alert, MapStopRoute.matching(route)),
                ),
                RouteCardData.Leaf(
                        RouteCardData.LineOrRoute.Route(route),
                        stop1,
                        0,
                        listOf(rp1, rp2),
                        setOf(stop1.id),
                        listOf(UpcomingTrip(representativeTrip, schedule)),
                        listOf(alert),
                        true,
                        true,
                        emptyList(),
                        anyEnumValueExcept(RouteCardData.Context.StopDetailsFiltered),
                    )
                    .format(now, GlobalResponse(objects)),
            )
        }

    @Test
    fun `filters alerts by trip`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop()
        val trip1 = objects.trip()
        val trip2 = objects.trip()
        val trip3 = objects.trip()
        val generalAlert =
            objects.alert { informedEntity(listOf(Alert.InformedEntity.Activity.Board)) }
        val trip1Alert =
            objects.alert {
                informedEntity(listOf(Alert.InformedEntity.Activity.Board), trip = trip1.id)
            }
        val trip2Alert =
            objects.alert {
                informedEntity(listOf(Alert.InformedEntity.Activity.Board), trip = trip2.id)
            }
        val leaf =
            RouteCardData.Leaf(
                RouteCardData.LineOrRoute.Route(route),
                stop,
                directionId = 0,
                routePatterns = emptyList(),
                stopIds = emptySet(),
                upcomingTrips = listOf(),
                alertsHere = listOf(generalAlert, trip1Alert, trip2Alert),
                hasSchedulesToday = true,
                allDataLoaded = true,
                alertsDownstream = listOf(generalAlert, trip1Alert, trip2Alert),
                context = RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(listOf(generalAlert, trip1Alert, trip2Alert), leaf.alertsHere(tripId = null))
        assertEquals(listOf(generalAlert, trip1Alert), leaf.alertsHere(tripId = trip1.id))
        assertEquals(listOf(generalAlert, trip2Alert), leaf.alertsHere(tripId = trip2.id))
        assertEquals(listOf(generalAlert), leaf.alertsHere(tripId = trip3.id))
        assertEquals(
            listOf(generalAlert, trip1Alert, trip2Alert),
            leaf.alertsDownstream(tripId = null),
        )
        assertEquals(listOf(generalAlert, trip1Alert), leaf.alertsDownstream(tripId = trip1.id))
        assertEquals(listOf(generalAlert, trip2Alert), leaf.alertsDownstream(tripId = trip2.id))
        assertEquals(listOf(generalAlert), leaf.alertsDownstream(tripId = trip3.id))
    }
}
