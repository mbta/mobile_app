package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.vehicle
import com.mbta.tid.mbta_app.model.UpcomingTrip.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock

class UpcomingTripTest {
    class FormatTest {
        @Test
        fun `status is non-null`() {
            assertEquals(
                Format.Overridden("Custom Text"),
                UpcomingTrip(prediction { status = "Custom Text" }).format(Clock.System.now())
            )
        }

        @Test
        fun `departure_time is null`() {
            assertEquals(
                Format.Hidden,
                UpcomingTrip(prediction { departureTime = null }).format(Clock.System.now())
            )
        }

        @Test
        fun `departure_time in the past`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Hidden,
                UpcomingTrip(
                        prediction {
                            arrivalTime = null
                            departureTime = now.minus(2.seconds)
                        }
                    )
                    .format(now)
            )
        }

        @Test
        fun `seconds less than 0`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Arriving,
                UpcomingTrip(
                        prediction {
                            arrivalTime = now.minus(2.seconds)
                            departureTime = now.plus(10.seconds)
                        }
                    )
                    .format(now)
            )
        }

        @Test
        fun boarding() {
            val now = Clock.System.now()
            val vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip1"
            }
            assertEquals(
                Format.Boarding,
                UpcomingTrip(
                        prediction {
                            departureTime = now.plus(10.seconds)
                            stopId = "12345"
                            tripId = "trip1"
                            vehicleId = vehicle.id
                        },
                        vehicle
                    )
                    .format(now)
            )
        }

        @Test
        fun `not boarding`() {
            val now = Clock.System.now()
            // wrong vehicle status
            var vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                stopId = "12345"
                tripId = "trip1"
            }
            assertNotEquals(
                Format.Boarding,
                UpcomingTrip(
                        prediction {
                            departureTime = now.plus(10.seconds)
                            stopId = "12345"
                            tripId = "trip1"
                            vehicleId = vehicle.id
                        },
                        vehicle
                    )
                    .format(now)
            )
            // wrong stop ID
            vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "67890"
                tripId = "trip1"
            }
            assertNotEquals(
                Format.Boarding,
                UpcomingTrip(
                        prediction {
                            departureTime = now.plus(10.seconds)
                            stopId = "12345"
                            tripId = "trip1"
                            vehicleId = vehicle.id
                        },
                        vehicle
                    )
                    .format(now)
            )
            // wrong trip ID
            vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip2"
            }
            assertNotEquals(
                Format.Boarding,
                UpcomingTrip(
                        prediction {
                            departureTime = now.plus(10.seconds)
                            stopId = "12345"
                            tripId = "trip1"
                            vehicleId = vehicle.id
                        },
                        vehicle
                    )
                    .format(now)
            )
        }

        @Test
        fun `seconds less than 30`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Arriving,
                UpcomingTrip(
                        prediction {
                            arrivalTime = now.plus(10.seconds)
                            departureTime = now.plus(20.seconds)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.Arriving,
                UpcomingTrip(prediction { departureTime = now.plus(15.seconds) }).format(now)
            )
        }

        @Test
        fun `seconds less than 60`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Approaching,
                UpcomingTrip(
                        prediction {
                            arrivalTime = now.plus(45.seconds)
                            departureTime = now.plus(50.seconds)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.Approaching,
                UpcomingTrip((prediction { departureTime = now.plus(40.seconds) })).format(now)
            )
        }

        @Test
        fun `minutes in the distant future`() {
            val now = Clock.System.now()
            val future = now.plus(DISTANT_FUTURE_CUTOFF).plus(1.minutes)
            val moreFuture = future.plus(38.minutes)

            assertEquals(
                Format.DistantFuture(future),
                UpcomingTrip(
                        prediction {
                            arrivalTime = future
                            departureTime = future.plus(1.minutes)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.DistantFuture(moreFuture),
                UpcomingTrip(prediction { departureTime = moreFuture }).format(now)
            )
        }

        @Test
        fun `minutes less than 20`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Minutes(1),
                UpcomingTrip(prediction { departureTime = now.plus(89.seconds) }).format(now)
            )
            assertEquals(
                Format.Minutes(2),
                UpcomingTrip(prediction { departureTime = now.plus(90.seconds) }).format(now)
            )
            assertEquals(
                Format.Minutes(2),
                UpcomingTrip(prediction { departureTime = now.plus(149.seconds) }).format(now)
            )
            assertEquals(
                Format.Minutes(3),
                UpcomingTrip(prediction { departureTime = now.plus(150.seconds) }).format(now)
            )
            assertEquals(
                Format.Minutes(3),
                UpcomingTrip(prediction { departureTime = now.plus(209.seconds) }).format(now)
            )
            assertEquals(
                Format.Minutes(45),
                UpcomingTrip((prediction { departureTime = now.plus(45.minutes) })).format(now)
            )
        }
    }
}
