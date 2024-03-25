package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class PatternsByHeadsignTest {
    @Test
    fun `formats as loading when null trips`() {
        val now = Clock.System.now()

        assertEquals(
            PatternsByHeadsign.Format.Loading,
            PatternsByHeadsign("", emptyList(), null, null).format(now)
        )
    }

    @Test
    fun `formats as alert with no trips and alert`() {
        val now = Clock.System.now()

        val alert = alert {}

        assertEquals(
            PatternsByHeadsign.Format.NoService(alert),
            PatternsByHeadsign("", emptyList(), emptyList(), listOf(alert)).format(now)
        )
    }

    @Test
    fun `formats as none with no trips and no alert`() {
        val now = Clock.System.now()

        assertEquals(
            PatternsByHeadsign.Format.None,
            PatternsByHeadsign("", emptyList(), emptyList(), emptyList()).format(now)
        )
    }

    @Test
    fun `skips trips that should be hidden`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

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

        val upcomingTrip1 = UpcomingTrip(prediction1)
        val upcomingTrip2 = UpcomingTrip(prediction2)

        assertEquals(
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        trip2.id,
                        UpcomingTrip.Format.Minutes(5)
                    )
                )
            ),
            PatternsByHeadsign("", emptyList(), listOf(upcomingTrip1, upcomingTrip2)).format(now)
        )
    }
}
