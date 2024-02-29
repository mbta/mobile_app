package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.TestData.prediction
import com.mbta.tid.mbta_app.TestData.trip
import com.mbta.tid.mbta_app.TestData.vehicle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock

class PredictionTest {
    class FormatTest {
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
        fun boarding() {
            val now = Clock.System.now()
            assertEquals(
                Prediction.Format.Boarding,
                prediction(
                        departureTime = now.plus(10.seconds),
                        stopId = "12345",
                        trip = trip(id = "trip1"),
                        vehicle =
                            vehicle(
                                currentStatus = Vehicle.CurrentStatus.StoppedAt,
                                stopId = "12345",
                                tripId = "trip1"
                            )
                    )
                    .format(now)
            )
        }

        @Test
        fun `not boarding`() {
            val now = Clock.System.now()
            // wrong vehicle status
            assertNotEquals(
                Prediction.Format.Boarding,
                prediction(
                        departureTime = now.plus(10.seconds),
                        stopId = "12345",
                        trip = trip(id = "trip1"),
                        vehicle =
                            vehicle(
                                currentStatus = Vehicle.CurrentStatus.IncomingAt,
                                stopId = "12345",
                                tripId = "trip1"
                            )
                    )
                    .format(now)
            )
            // wrong stop ID
            assertNotEquals(
                Prediction.Format.Boarding,
                prediction(
                        departureTime = now.plus(10.seconds),
                        stopId = "12345",
                        trip = trip(id = "trip"),
                        vehicle =
                            vehicle(
                                currentStatus = Vehicle.CurrentStatus.StoppedAt,
                                stopId = "67890",
                                tripId = "trip1"
                            )
                    )
                    .format(now)
            )
            // wrong trip ID
            assertNotEquals(
                Prediction.Format.Boarding,
                prediction(
                        departureTime = now.plus(10.seconds),
                        stopId = "12345",
                        trip = trip(id = "trip1"),
                        vehicle =
                            vehicle(
                                currentStatus = Vehicle.CurrentStatus.StoppedAt,
                                stopId = "12345",
                                tripId = "trip2"
                            )
                    )
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
        fun `minutes in the distant future`() {
            val now = Clock.System.now()
            val future = now.plus(DISTANT_FUTURE_CUTOFF).plus(1.minutes)
            val moreFuture = future.plus(38.minutes)
            assertEquals(
                Prediction.Format.DistantFuture(future),
                prediction(arrivalTime = future, departureTime = future.plus(1.minutes)).format(now)
            )
            assertEquals(
                Prediction.Format.DistantFuture(moreFuture),
                prediction(departureTime = moreFuture).format(now),
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
                Prediction.Format.Minutes(45),
                prediction(departureTime = now.plus(45.minutes)).format(now)
            )
        }
    }
}
