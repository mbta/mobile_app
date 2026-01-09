package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import com.mbta.tid.mbta_app.parametric.parametricTest
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class LeafFormatTest {
    @Test
    fun `Single tileData returns trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val lastTrip = anyBoolean()
        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Arriving(lastTrip),
                lastTrip,
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Minutes(10, lastTrip),
                lastTrip,
            )
        val trip3 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleTime(
                    EasternTimeInstant(Instant.DISTANT_FUTURE),
                    lastTrip,
                ),
                lastTrip,
            )
        val format =
            LeafFormat.Single(
                route = null,
                "Overridden Headsign",
                UpcomingFormat.Some(listOf(trip1, trip2, trip3), null),
            )
        assertEquals(
            listOf(
                TileData(null, "Overridden Headsign", UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(null, "Overridden Headsign", UpcomingFormat.Some(trip2, null), trip2.trip),
                TileData(null, "Overridden Headsign", UpcomingFormat.Some(trip3, null), trip3.trip),
            ),
            format.tileData(directionDestination = null),
        )
    }

    @Test
    fun `Single tileData drops headsign if normal`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val lastTrip = anyBoolean()
        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Arriving(lastTrip),
                lastTrip,
            )
        val format = LeafFormat.Single(null, "Headsign", UpcomingFormat.Some(listOf(trip1), null))
        assertEquals(
            listOf(TileData(null, null, UpcomingFormat.Some(trip1, null), trip1.trip)),
            format.tileData(directionDestination = "Headsign"),
        )
    }

    @Test
    fun `Branched tileData returns trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val route1 = objects.route { type = routeType }
        val route2 = objects.route { type = routeType }
        val lastTrip = anyBoolean()

        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Approaching(lastTrip),
                lastTrip,
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleMinutes(35, lastTrip),
                lastTrip,
            )
        val trip3 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Now(lastTrip),
                lastTrip,
            )

        val format =
            LeafFormat.branched {
                branchRow(route1, "Headsign 1", UpcomingFormat.Some(trip1, null))
                branchRow(route2, "Headsign 2", UpcomingFormat.Some(trip2, null))
                branchRow(route1, "Headsign 3", UpcomingFormat.Some(trip3, null))
            }
        assertEquals(
            listOf(
                TileData(route1, "Headsign 1", UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(route2, "Headsign 2", UpcomingFormat.Some(trip2, null), trip2.trip),
                TileData(route1, "Headsign 3", UpcomingFormat.Some(trip3, null), trip3.trip),
            ),
            format.tileData(directionDestination = null),
        )
    }

    @Test
    fun `Branched tileData ignores branches without trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val lastTrip = anyBoolean()

        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Approaching(lastTrip),
                lastTrip,
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleMinutes(35, lastTrip),
                lastTrip,
            )

        val format =
            LeafFormat.branched {
                branchRow("Headsign 1", UpcomingFormat.Some(trip1, null))
                branchRow("Headsign 2", UpcomingFormat.Some(trip2, null))
                branchRow("Headsign 3", UpcomingFormat.Loading)
            }
        assertEquals(
            listOf(
                TileData(null, "Headsign 1", UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(null, "Headsign 2", UpcomingFormat.Some(trip2, null), trip2.trip),
            ),
            format.tileData(directionDestination = null),
        )
    }

    @Test
    fun `Single tileData empty if no trips`() {
        val format =
            LeafFormat.Single(
                route = null,
                headsign = null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday),
            )
        assertEquals(emptyList(), format.tileData(directionDestination = null))
    }

    @Test
    fun `Branched tileData empty if no trips`() {
        val format =
            LeafFormat.branched {
                branchRow(
                    "Headsign 1",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
                )
                branchRow("Headsign 2", UpcomingFormat.Loading)
            }
        assertEquals(emptyList(), format.tileData(directionDestination = null))
    }

    @Test
    fun `Single noPredictionsStatus returns status if no predictions`() {
        for (noTrips in
            listOf(
                UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                UpcomingFormat.NoTripsFormat.NoSchedulesToday,
            )) {
            val format =
                LeafFormat.Single(route = null, headsign = null, UpcomingFormat.NoTrips(noTrips))
            assertEquals(noTrips, format.noPredictionsStatus())
        }
    }

    @Test
    fun `Branched noPredictionsStatus returns any status if no predictions`() {
        val format =
            LeafFormat.branched {
                branchRow("Headsign 2", UpcomingFormat.Loading)
                branchRow(
                    "Headsign 1",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday),
                )
                branchRow(
                    "Headsign 2",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
                )
            }
        assertEquals(UpcomingFormat.NoTripsFormat.ServiceEndedToday, format.noPredictionsStatus())
    }

    @Test
    fun `Single noPredictionsStatus returns null if predictions`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val lastTrip = anyBoolean()
        val trip =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                anyEnumValue(),
                TripInstantDisplay.Minutes(15, lastTrip),
                lastTrip,
            )
        val format =
            LeafFormat.Single(route = null, headsign = null, UpcomingFormat.Some(trip, null))
        assertNull(format.noPredictionsStatus())
    }

    @Test
    fun `Branched noPredictionsStatus returns null if any predictions`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val lastTrip = anyBoolean()
        val trip =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                anyEnumValue(),
                TripInstantDisplay.Now(lastTrip),
                lastTrip,
            )
        val format =
            LeafFormat.branched {
                branchRow("Headsign 1", UpcomingFormat.Loading)
                branchRow(
                    "Headsign 2",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday),
                )
                branchRow("Headsign 3", UpcomingFormat.Some(trip, null))
            }
        assertNull(format.noPredictionsStatus())
    }
}
