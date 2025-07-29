package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TileDataTest {
    @Test
    fun `id matches upcoming trip id + stopSequence`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val prediction = objects.prediction { stopSequence = 1 }
        val upcomingTrip = UpcomingTrip(objects.trip(), prediction)
        val format =
            UpcomingFormat.Some(
                UpcomingFormat.Some.FormattedTrip(
                    upcomingTrip,
                    route.type,
                    TripInstantDisplay.Arriving,
                ),
                null,
            )
        val tileData = TileData(route, "Headsign", format, upcomingTrip)

        assertEquals("${upcomingTrip.trip.id}-1", tileData.id)
    }

    @Test
    fun `fromUpcoming keeps arriving trip`() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip { headsign = "Headsign" }
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 30.seconds
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        assertEquals(
            TileData(
                route,
                "Headsign",
                UpcomingFormat.Some(
                    UpcomingFormat.Some.FormattedTrip(
                        upcomingTrip,
                        route.type,
                        TripInstantDisplay.Arriving,
                    ),
                    null,
                ),
                upcomingTrip,
            ),
            TileData.fromUpcoming(upcomingTrip, route, now),
        )
    }

    @Test
    fun `fromUpcoming drops hidden trip`() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now - 3.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        assertNull(TileData.fromUpcoming(upcomingTrip, route, now))
    }

    @Test
    fun `isSelected is true when the filter matches`() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 5.minutes
                stopSequence = 2
            }
        val upcomingTrip = objects.upcomingTrip(prediction)
        val tileData = TileData.fromUpcoming(upcomingTrip, route, now)!!

        assertFalse(tileData.isSelected(null))
        assertFalse(tileData.isSelected(TripDetailsFilter("other-trip", null, null)))
        assertFalse(tileData.isSelected(TripDetailsFilter(trip.id, null, 0)))

        assertTrue(tileData.isSelected(TripDetailsFilter(trip.id, null, null)))
        assertTrue(tileData.isSelected(TripDetailsFilter(trip.id, null, prediction.stopSequence)))
    }
}
