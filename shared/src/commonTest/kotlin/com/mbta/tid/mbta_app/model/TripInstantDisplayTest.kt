package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock

class TripInstantDisplayTest {
    @Test
    fun `status is non-null`() {
        assertEquals(
            TripInstantDisplay.Overridden("Custom Text"),
            TripInstantDisplay.from(
                prediction = ObjectCollectionBuilder.Single.prediction { status = "Custom Text" },
                schedule = null,
                vehicle = null,
                now = Clock.System.now(),
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `scheduled trip skipped`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Skipped(now + 15.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Skipped
                    },
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `unscheduled trip skipped`() {
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Skipped
                    },
                schedule = null,
                vehicle = null,
                now = Clock.System.now(),
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `departure_time and arrival_time are null`() {
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = null
                        arrivalTime = null
                    },
                schedule = null,
                vehicle = null,
                now = Clock.System.now(),
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule {
                        departureTime = null
                        arrivalTime = null
                    },
                vehicle = null,
                now = Clock.System.now(),
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `departure_time is null but arrival_time exists`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(3),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = true
            )
        )
        assertEquals(
            TripInstantDisplay.Schedule(now + 3.minutes),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                vehicle = null,
                now = now,
                allowArrivalOnly = true
            )
        )
    }

    @Test
    fun `schedule instead of prediction`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Schedule(now + 15.minutes),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `departure_time in the past`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = null
                        departureTime = now.minus(2.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `seconds less than 0`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Arriving,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.minus(2.seconds)
                        departureTime = now.plus(10.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun boarding() {
        val now = Clock.System.now()
        val vehicle =
            ObjectCollectionBuilder.Single.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip1"
            }
        assertEquals(
            TripInstantDisplay.Boarding,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(10.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `not boarding when stopped at stop but more than 90 seconds until departure`() {
        val now = Clock.System.now()
        val vehicle =
            ObjectCollectionBuilder.Single.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip1"
            }
        assertEquals(
            TripInstantDisplay.Minutes(2),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(95.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `not boarding`() {
        val now = Clock.System.now()
        // wrong vehicle status
        var vehicle =
            ObjectCollectionBuilder.Single.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                stopId = "12345"
                tripId = "trip1"
            }
        assertNotEquals(
            TripInstantDisplay.Boarding,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(10.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                allowArrivalOnly = false
            )
        )
        // wrong stop ID
        vehicle =
            ObjectCollectionBuilder.Single.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "67890"
                tripId = "trip1"
            }
        assertNotEquals(
            TripInstantDisplay.Boarding,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(10.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                allowArrivalOnly = false
            )
        )
        // wrong trip ID
        vehicle =
            ObjectCollectionBuilder.Single.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip2"
            }
        assertNotEquals(
            TripInstantDisplay.Boarding,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(10.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `seconds less than 30`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Arriving,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.plus(10.seconds)
                        departureTime = now.plus(20.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Arriving,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(15.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `seconds less than 60`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Approaching,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.plus(45.seconds)
                        departureTime = now.plus(50.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Approaching,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(40.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `minutes in the distant future`() {
        val now = Clock.System.now()
        val future = now.plus(DISTANT_FUTURE_CUTOFF).plus(1.minutes)
        val moreFuture = future.plus(38.minutes)

        assertEquals(
            TripInstantDisplay.DistantFuture(future),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = future
                        departureTime = future.plus(1.minutes)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.DistantFuture(moreFuture),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction { departureTime = moreFuture },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }

    @Test
    fun `minutes less than 20`() {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Minutes(1),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(89.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(2),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(90.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(2),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(149.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(3),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(150.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(3),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(209.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(45),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(45.minutes)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                allowArrivalOnly = false
            )
        )
    }
}
