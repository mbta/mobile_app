package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock

class TripInstantDisplayTest {
    @Test
    fun `status is non-null`() = parametricTest {
        assertEquals(
            TripInstantDisplay.Overridden("Custom Text"),
            TripInstantDisplay.from(
                prediction = ObjectCollectionBuilder.Single.prediction { status = "Custom Text" },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = Clock.System.now(),
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `scheduled trip skipped`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `unscheduled trip skipped`() = parametricTest {
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Skipped
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = Clock.System.now(),
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `departure_time and arrival_time are null`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = Clock.System.now(),
                context = anyEnumValue()
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
                routeType = anyEnumValue(),
                now = Clock.System.now(),
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `departure_time is null but arrival_time exists`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
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
                routeType = anyEnumValue(),
                context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
            )
        )
        assertEquals(
            TripInstantDisplay.AsTime(now + 3.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
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
                routeType = anyEnumValue(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
            )
        )
    }

    @Test
    fun `schedule instead of prediction`() = parametricTest {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.Schedule(now + 15.minutes),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `departure_time in the past`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `seconds less than 0`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun boarding() = parametricTest {
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
                routeType = anyEnumValue(),
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `not boarding when stopped at stop but more than 90 seconds until departure`() =
        parametricTest {
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
                    routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                    now = now,
                    context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
                )
            )
        }

    @Test
    fun `not boarding`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
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
                routeType = anyEnumValue(),
                vehicle = vehicle,
                now = now,
                context = anyEnumValue()
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `seconds less than 30`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValue()
            )
        )
    }

    @Test
    fun `seconds less than 60 outside trip details`() = parametricTest {
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
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
                routeType = anyEnumValue(),
                now = now,
                context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
            )
        )
    }

    @Test
    fun `seconds less than 60 in trip details`() = parametricTest {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.AsTime(now + 45.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.plus(45.seconds)
                        departureTime = now.plus(50.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
            )
        )
        assertEquals(
            TripInstantDisplay.AsTime(now + 40.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(40.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
            )
        )
    }

    @Test
    fun `minutes in the distant future`() = parametricTest {
        val now = Clock.System.now()
        val context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)

        val futureMinutes = 61
        val moreFutureMinutes = futureMinutes + 38

        assertEquals(
            TripInstantDisplay.Minutes(futureMinutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.plus(futureMinutes.minutes)
                        departureTime = now.plus(futureMinutes.minutes).plus(1.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValue(),
                now = now,
                context = context
            )
        )
        assertEquals(
            TripInstantDisplay.Minutes(moreFutureMinutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(moreFutureMinutes.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
            )
        )
    }

    @Test
    fun `minutes less than 20 outside trip details`() = parametricTest {
        val now = Clock.System.now()
        val context = anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)
        assertEquals(
            TripInstantDisplay.Minutes(1),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(89.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
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
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
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
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
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
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
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
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
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
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = context
            )
        )
    }

    @Test
    fun `minutes less than 20 in trip details`() = parametricTest {
        val now = Clock.System.now()
        assertEquals(
            TripInstantDisplay.AsTime(now + 90.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(90.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
            )
        )
        assertEquals(
            TripInstantDisplay.AsTime(now + 149.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(149.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                context = TripInstantDisplay.Context.TripDetails
            )
        )
        assertEquals(
            TripInstantDisplay.AsTime(now + 45.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(45.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = now,
                context = TripInstantDisplay.Context.TripDetails
            )
        )
    }
}
