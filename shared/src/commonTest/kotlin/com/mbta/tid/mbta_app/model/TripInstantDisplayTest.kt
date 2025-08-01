package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TripInstantDisplayTest {

    private fun ParametricTest.subway() =
        anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY, RouteType.BUS)

    private fun ParametricTest.scheduleBased() = anyOf(RouteType.COMMUTER_RAIL, RouteType.FERRY)

    private fun ParametricTest.nonScheduleBased() =
        anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY)

    private fun ParametricTest.nonTripDetails() =
        anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)

    @Test
    fun `status is non-null`() = parametricTest {
        assertEquals(
            TripInstantDisplay.Overridden("Custom Text"),
            TripInstantDisplay.from(
                prediction = ObjectCollectionBuilder.Single.prediction { status = "Custom Text" },
                schedule = null,
                vehicle = null,
                routeType = anyEnumValueExcept(RouteType.COMMUTER_RAIL),
                now = EasternTimeInstant.now(),
                context = anyEnumValue(),
            ),
        )
    }

    @Test
    fun `commuter rail status is non-null and prediction is in past`() {
        val now = EasternTimeInstant.now()
        val predictionTime = now - 3.minutes
        assertEquals(
            TripInstantDisplay.TimeWithStatus(predictionTime, "Custom Text", headline = true),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = predictionTime
                        status = "Custom Text"
                    },
                schedule = null,
                vehicle = null,
                routeType = RouteType.COMMUTER_RAIL,
                now = now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `commuter rail status is non-null, prediction time null, schedule in past`() {
        val now = EasternTimeInstant.now()
        val scheduleTime = now - 3.minutes
        assertEquals(
            TripInstantDisplay.ScheduleTimeWithStatus(scheduleTime, "Custom Text", headline = true),
            TripInstantDisplay.from(
                prediction = ObjectCollectionBuilder.Single.prediction { status = "Custom Text" },
                schedule = ObjectCollectionBuilder.Single.schedule { departureTime = scheduleTime },
                vehicle = null,
                routeType = RouteType.COMMUTER_RAIL,
                now = now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `scheduled trip skipped`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = anyEnumValue(),
            ),
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
                routeType = null,
                now = EasternTimeInstant.now(),
                context = anyEnumValue(),
            ),
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
                routeType = null,
                now = EasternTimeInstant.now(),
                context = anyEnumValue(),
            ),
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
                routeType = null,
                now = EasternTimeInstant.now(),
                context = anyEnumValue(),
            ),
        )
    }

    @Test
    fun `departure_time is null but arrival_time exists`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
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
                routeType = null,
                context = nonTripDetails(),
            ),
        )
        assertEquals(
            TripInstantDisplay.Time(now + 3.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                schedule = null,
                vehicle = null,
                routeType = nonScheduleBased(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
        assertEquals(
            TripInstantDisplay.ScheduleTime(now + 3.minutes),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule {
                        departureTime = null
                        arrivalTime = now + 3.minutes
                    },
                vehicle = null,
                routeType = nonScheduleBased(),
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
    }

    @Test
    fun `schedule instead of prediction`() = parametricTest {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.ScheduleMinutes(15),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = nonTripDetails(),
            ),
        )

        assertEquals(
            TripInstantDisplay.ScheduleTime(now + 75.minutes, false),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 75.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `schedule rounding`() = parametricTest {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.ScheduleMinutes(59),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule {
                        departureTime = now + 59.4999.minutes
                    },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = nonTripDetails(),
            ),
        )
        assertEquals(
            TripInstantDisplay.ScheduleTime(now + 59.5.minutes),
            TripInstantDisplay.from(
                prediction = null,
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 59.5.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `departure_time in the past`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = anyEnumValue(),
            ),
        )
    }

    @Test
    fun `seconds less than 0`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun boarding() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `still boarding if prediction in past`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                        departureTime = now.minus(10.seconds)
                        stopId = "12345"
                        tripId = "trip1"
                        vehicleId = vehicle.id
                    },
                schedule = null,
                vehicle = vehicle,
                now = now,
                routeType = null,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `not boarding when stopped at stop but more than 90 seconds until departure`() =
        parametricTest {
            val now = EasternTimeInstant.now()
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
                    routeType = nonScheduleBased(),
                    now = now,
                    context = nonTripDetails(),
                ),
            )
        }

    @Test
    fun `boarding when subway stopped at stop but arrival is in the past and more than 90 seconds until departure`() =
        parametricTest {
            val now = EasternTimeInstant.now()
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
                            arrivalTime = now.minus(5.seconds)
                            departureTime = now.plus(95.seconds)
                            stopId = "12345"
                            tripId = "trip1"
                            vehicleId = vehicle.id
                        },
                    schedule = null,
                    vehicle = vehicle,
                    routeType = subway(),
                    now = now,
                    context = nonTripDetails(),
                ),
            )
        }

    @Test
    fun `not boarding`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = anyEnumValue(),
            ),
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
                routeType = null,
                vehicle = vehicle,
                now = now,
                context = anyEnumValue(),
            ),
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
                routeType = null,
                now = now,
                context = anyEnumValue(),
            ),
        )
    }

    @Test
    fun `seconds less than 30`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `arriving when prediction arrival in the past and departure more than 30 seconds away`() =
        parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Arriving,
                TripInstantDisplay.from(
                    prediction =
                        ObjectCollectionBuilder.Single.prediction {
                            arrivalTime = now.minus(10.seconds)
                            departureTime = now.plus(40.seconds)
                        },
                    schedule = null,
                    vehicle = null,
                    routeType = null,
                    now = now,
                    context = nonTripDetails(),
                ),
            )
        }

    @Test
    fun `seconds less than 60 outside trip details`() = parametricTest {
        val now = EasternTimeInstant.now()
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
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
                routeType = null,
                now = now,
                context = nonTripDetails(),
            ),
        )
    }

    @Test
    fun `seconds less than 60 in trip details`() {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Time(now + 45.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        arrivalTime = now.plus(45.seconds)
                        departureTime = now.plus(50.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = null,
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
        assertEquals(
            TripInstantDisplay.Time(now + 40.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(40.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = null,
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
    }

    @Test
    fun `minutes in the distant future`() = parametricTest {
        val now = EasternTimeInstant.now()
        val context = nonTripDetails()

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
                routeType = null,
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
        )
        assertEquals(
            TripInstantDisplay.Time(now.plus(moreFutureMinutes.minutes), true),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(moreFutureMinutes.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = scheduleBased(),
                now = now,
                context = context,
            ),
        )
    }

    @Test
    fun `time with status`() {
        val now = EasternTimeInstant.now()
        val predictionTime = now + 2.minutes
        val prediction =
            ObjectCollectionBuilder.Single.prediction {
                status = "All aboard"
                departureTime = predictionTime
            }

        assertEquals(
            TripInstantDisplay.TimeWithStatus(predictionTime, "All aboard", true),
            TripInstantDisplay.from(
                prediction,
                schedule = null,
                vehicle = null,
                routeType = RouteType.COMMUTER_RAIL,
                now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `time with schedule early`() {
        val now = EasternTimeInstant.now()
        val predictionTime = now + 2.minutes
        val scheduleTime = now + 5.minutes
        val prediction =
            ObjectCollectionBuilder.Single.prediction { departureTime = predictionTime }

        val schedule = ObjectCollectionBuilder.Single.schedule { departureTime = scheduleTime }

        assertEquals(
            TripInstantDisplay.TimeWithSchedule(predictionTime, scheduleTime, true),
            TripInstantDisplay.from(
                prediction,
                schedule,
                vehicle = null,
                routeType = RouteType.COMMUTER_RAIL,
                now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `time with schedule late`() {
        val now = EasternTimeInstant.now()
        val predictionTime = now + 2.minutes
        val scheduleTime = now - 5.minutes
        val prediction =
            ObjectCollectionBuilder.Single.prediction { departureTime = predictionTime }

        val schedule = ObjectCollectionBuilder.Single.schedule { departureTime = scheduleTime }

        assertEquals(
            TripInstantDisplay.TimeWithSchedule(predictionTime, scheduleTime, true),
            TripInstantDisplay.from(
                prediction,
                schedule,
                vehicle = null,
                routeType = RouteType.COMMUTER_RAIL,
                now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `minutes less than 20 outside trip details`() = parametricTest {
        val now = EasternTimeInstant.now()
        val context = nonTripDetails()
        assertEquals(
            TripInstantDisplay.Minutes(1),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(89.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
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
                routeType = nonScheduleBased(),
                now = now,
                context = context,
            ),
        )

        assertEquals(
            TripInstantDisplay.Time(predictionTime = now.plus(45.minutes), true),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(45.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = scheduleBased(),
                now = now,
                context = context,
            ),
        )
    }

    @Test
    fun `minutes less than 20 in trip details`() {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Time(now + 90.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(90.seconds)
                    },
                schedule = null,
                vehicle = null,
                routeType = null,
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
        assertEquals(
            TripInstantDisplay.Time(now + 149.seconds),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(149.seconds)
                    },
                schedule = null,
                vehicle = null,
                now = now,
                routeType = null,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
        assertEquals(
            TripInstantDisplay.Time(now + 45.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        departureTime = now.plus(45.minutes)
                    },
                schedule = null,
                vehicle = null,
                routeType = null,
                now = now,
                context = TripInstantDisplay.Context.TripDetails,
            ),
        )
    }

    @Test
    fun `scheduled trip cancelled`() {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Cancelled(now + 15.minutes),
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
                        arrivalTime = null
                        departureTime = null
                    },
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `scheduled trip cancelled in past is hidden`() {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
                        arrivalTime = null
                        departureTime = null
                    },
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now - 15.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `cancelled subway trip is hidden`() {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
                        arrivalTime = null
                        departureTime = null
                    },
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                routeType = RouteType.LIGHT_RAIL,
                now = now,
                context = TripInstantDisplay.Context.StopDetailsFiltered,
            ),
        )
    }

    @Test
    fun `cancelled trip is hidden in other contexts`() = parametricTest {
        val now = EasternTimeInstant.now()
        assertEquals(
            TripInstantDisplay.Hidden,
            TripInstantDisplay.from(
                prediction =
                    ObjectCollectionBuilder.Single.prediction {
                        scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
                        arrivalTime = null
                        departureTime = null
                    },
                schedule =
                    ObjectCollectionBuilder.Single.schedule { departureTime = now + 15.minutes },
                vehicle = null,
                routeType = RouteType.BUS,
                now = now,
                context = anyEnumValueExcept(TripInstantDisplay.Context.StopDetailsFiltered),
            ),
        )
    }
}
