package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class PredictionTest {
    class FormatTest {
        fun prediction(
            id: String = "",
            arrivalTime: Instant? = null,
            departureTime: Instant? = null,
            directionId: Int = 0,
            revenue: Boolean = true,
            scheduleRelationship: Prediction.ScheduleRelationship =
                Prediction.ScheduleRelationship.Scheduled,
            status: String? = null,
            stopSequence: Int? = null,
            trip: Trip = Trip("", "", listOf())
        ) =
            Prediction(
                id,
                arrivalTime,
                departureTime,
                directionId,
                revenue,
                scheduleRelationship,
                status,
                stopSequence,
                trip
            )

        @Test
        fun `status is non-null`() {
            assertEquals(
                Prediction.Format.Overridden("Custom Text"),
                prediction(status = "Custom Text").format(Clock.System.now())
            )
        }

        @Test
        fun `departure_time is null`() {
            assertEquals(
                Prediction.Format.Hidden,
                prediction(departureTime = null).format(Clock.System.now())
            )
        }

        @Test
        fun `departure_time in the past`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Hidden,
                prediction(arrivalTime = null, departureTime = now.minus(2.seconds)).format(now)
            )
        }

        @Test
        fun `seconds less than 0`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Arriving,
                prediction(arrivalTime = now.minus(2.seconds), departureTime = now.plus(10.seconds))
                    .format(now)
            )
        }

        @Test
        fun `seconds less than 30`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Arriving,
                prediction(arrivalTime = now.plus(10.seconds), departureTime = now.plus(20.seconds))
                    .format(now)
            )
            assertEquals(
                Prediction.Format.Arriving,
                prediction(departureTime = now.plus(15.seconds)).format(now)
            )
        }

        @Test
        fun `seconds less than 60`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Approaching,
                prediction(arrivalTime = now.plus(45.seconds), departureTime = now.plus(50.seconds))
                    .format(now)
            )
            assertEquals(
                Prediction.Format.Approaching,
                prediction(departureTime = now.plus(40.seconds)).format(now)
            )
        }

        @Test
        fun `minutes greater than 20`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.DistantFuture,
                prediction(arrivalTime = now.plus(25.minutes), departureTime = now.plus(26.minutes))
                    .format(now)
            )
            assertEquals(
                Prediction.Format.DistantFuture,
                prediction(departureTime = now.plus(21.minutes)).format(now),
            )
        }

        @Test
        fun `minutes less than 20`() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Minutes(1),
                prediction(departureTime = now.plus(89.seconds)).format(now)
            )
            assertEquals(
                Prediction.Format.Minutes(2),
                prediction(departureTime = now.plus(90.seconds)).format(now)
            )
            assertEquals(
                Prediction.Format.Minutes(2),
                prediction(departureTime = now.plus(149.seconds)).format(now)
            )
            assertEquals(
                Prediction.Format.Minutes(3),
                prediction(departureTime = now.plus(150.seconds)).format(now)
            )
            assertEquals(
                Prediction.Format.Minutes(3),
                prediction(departureTime = now.plus(209.seconds)).format(now)
            )
            assertEquals(
                Prediction.Format.Minutes(10),
                prediction(departureTime = now.plus(10.minutes)).format(now)
            )
        }
    }
}
