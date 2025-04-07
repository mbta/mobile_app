package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant

class LeafFormatTest {
    @Test
    fun `Single tileData returns trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Arriving
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Minutes(10)
            )
        val trip3 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleTime(Instant.DISTANT_FUTURE)
            )
        val format =
            LeafFormat.Single(
                "Overridden Headsign",
                UpcomingFormat.Some(listOf(trip1, trip2, trip3), null)
            )
        assertEquals(
            listOf(
                TileData(null, null, UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(null, null, UpcomingFormat.Some(trip2, null), trip2.trip),
                TileData(null, null, UpcomingFormat.Some(trip3, null), trip3.trip),
            ),
            format.tileData()
        )
    }

    @Test
    fun `Branched tileData returns trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()
        val route1 = objects.route { type = routeType }
        val route2 = objects.route { type = routeType }

        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Approaching
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleMinutes(35)
            )
        val trip3 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Now
            )

        val format =
            LeafFormat.branched {
                branch(route1, "Headsign 1", UpcomingFormat.Some(trip1, null))
                branch(route2, "Headsign 2", UpcomingFormat.Some(trip2, null))
                branch(route1, "Headsign 3", UpcomingFormat.Some(trip3, null))
            }
        assertEquals(
            listOf(
                TileData(route1, "Headsign 1", UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(route2, "Headsign 2", UpcomingFormat.Some(trip2, null), trip2.trip),
                TileData(route1, "Headsign 3", UpcomingFormat.Some(trip3, null), trip3.trip),
            ),
            format.tileData()
        )
    }

    @Test
    fun `Branched tileData ignores branches without trips`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val routeType = anyEnumValue<RouteType>()

        val trip1 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.Approaching
            )
        val trip2 =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                routeType,
                TripInstantDisplay.ScheduleMinutes(35)
            )

        val format =
            LeafFormat.branched {
                branch("Headsign 1", UpcomingFormat.Some(trip1, null))
                branch("Headsign 2", UpcomingFormat.Some(trip2, null))
                branch("Headsign 3", UpcomingFormat.Loading)
            }
        assertEquals(
            listOf(
                TileData(null, "Headsign 1", UpcomingFormat.Some(trip1, null), trip1.trip),
                TileData(null, "Headsign 2", UpcomingFormat.Some(trip2, null), trip2.trip),
            ),
            format.tileData()
        )
    }

    @Test
    fun `Single tileData empty if no trips`() {
        val format =
            LeafFormat.Single(
                null,
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday)
            )
        assertEquals(emptyList(), format.tileData())
    }

    @Test
    fun `Branched tileData empty if no trips`() {
        val format =
            LeafFormat.branched {
                branch(
                    "Headsign 1",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable)
                )
                branch("Headsign 2", UpcomingFormat.Loading)
            }
        assertEquals(emptyList(), format.tileData())
    }

    @Test
    fun `Single noPredictionsStatus returns status if no predictions`() {
        for (noTrips in
            listOf(
                UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                UpcomingFormat.NoTripsFormat.NoSchedulesToday
            )) {
            val format = LeafFormat.Single(null, UpcomingFormat.NoTrips(noTrips))
            assertEquals(noTrips, format.noPredictionsStatus())
        }
    }

    @Test
    fun `Branched noPredictionsStatus returns any status if no predictions`() {
        val format =
            LeafFormat.branched {
                branch("Headsign 2", UpcomingFormat.Loading)
                branch(
                    "Headsign 1",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday)
                )
                branch(
                    "Headsign 2",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable)
                )
            }
        assertEquals(UpcomingFormat.NoTripsFormat.ServiceEndedToday, format.noPredictionsStatus())
    }

    @Test
    fun `Single noPredictionsStatus returns null if predictions`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val trip =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                anyEnumValue(),
                TripInstantDisplay.Minutes(15)
            )
        val format = LeafFormat.Single(null, UpcomingFormat.Some(trip, null))
        assertNull(format.noPredictionsStatus())
    }

    @Test
    fun `Branched noPredictionsStatus returns null if any predictions`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val trip =
            UpcomingFormat.Some.FormattedTrip(
                UpcomingTrip(objects.trip()),
                anyEnumValue(),
                TripInstantDisplay.Now
            )
        val format =
            LeafFormat.branched {
                branch("Headsign 1", UpcomingFormat.Loading)
                branch(
                    "Headsign 2",
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday)
                )
                branch("Headsign 3", UpcomingFormat.Some(trip, null))
            }
        assertNull(format.noPredictionsStatus())
    }
}
