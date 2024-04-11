package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.schedule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
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
                UpcomingTrip(trip {}, prediction { status = "Custom Text" })
                    .format(Clock.System.now())
            )
        }

        @Test
        fun `departure_time is null`() {
            assertEquals(
                Format.Hidden,
                UpcomingTrip(trip {}, prediction { departureTime = null })
                    .format(Clock.System.now())
            )
            assertEquals(
                Format.Hidden,
                UpcomingTrip(trip {}, schedule { departureTime = null }).format(Clock.System.now())
            )
        }

        @Test
        fun `schedule instead of prediction`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Schedule(now + 15.minutes),
                UpcomingTrip(trip {}, schedule { departureTime = now + 15.minutes }).format(now)
            )
        }

        @Test
        fun `departure_time in the past`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Hidden,
                UpcomingTrip(
                        trip {},
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
                        trip {},
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
                        trip {},
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
        fun `not boarding when stopped at stop but more than 90 seconds until departure`() {
            val now = Clock.System.now()
            val vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip1"
            }
            assertEquals(
                Format.Minutes(2),
                UpcomingTrip(
                        trip {},
                        prediction {
                            departureTime = now.plus(95.seconds)
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
                        trip {},
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
                        trip {},
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
                        trip {},
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
                        trip {},
                        prediction {
                            arrivalTime = now.plus(10.seconds)
                            departureTime = now.plus(20.seconds)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.Arriving,
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(15.seconds) })
                    .format(now)
            )
        }

        @Test
        fun `seconds less than 60`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Approaching,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.plus(45.seconds)
                            departureTime = now.plus(50.seconds)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.Approaching,
                UpcomingTrip(trip {}, (prediction { departureTime = now.plus(40.seconds) }))
                    .format(now)
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
                        trip {},
                        prediction {
                            arrivalTime = future
                            departureTime = future.plus(1.minutes)
                        }
                    )
                    .format(now)
            )
            assertEquals(
                Format.DistantFuture(moreFuture),
                UpcomingTrip(trip {}, prediction { departureTime = moreFuture }).format(now)
            )
        }

        @Test
        fun `minutes less than 20`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Minutes(1),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(89.seconds) })
                    .format(now)
            )
            assertEquals(
                Format.Minutes(2),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(90.seconds) })
                    .format(now)
            )
            assertEquals(
                Format.Minutes(2),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(149.seconds) })
                    .format(now)
            )
            assertEquals(
                Format.Minutes(3),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(150.seconds) })
                    .format(now)
            )
            assertEquals(
                Format.Minutes(3),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(209.seconds) })
                    .format(now)
            )
            assertEquals(
                Format.Minutes(45),
                UpcomingTrip(trip {}, (prediction { departureTime = now.plus(45.minutes) }))
                    .format(now)
            )
        }
    }

    @Test
    fun `tripsFromData matches things up`() {
        fun ObjectCollectionBuilder.ScheduleBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int
        ) {
            this.stopId = stopId
            this.tripId = tripId
            this.stopSequence = stopSequence
        }
        fun ObjectCollectionBuilder.PredictionBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int
        ) {
            this.stopId = stopId
            this.tripId = tripId
            this.stopSequence = stopSequence
        }

        val objects = ObjectCollectionBuilder()

        // schedule and prediction match
        val trip1 = objects.trip()
        val trip1Schedule = objects.schedule { sts("stop1", trip1.id, 1) }
        val trip1Prediction = objects.prediction { sts("stop1", trip1.id, 1) }

        // schedule, no prediction
        val trip2 = objects.trip()
        val trip2Schedule = objects.schedule { sts("stop2", trip2.id, 2) }

        // prediction, no schedule
        val trip3 = objects.trip()
        val trip3Prediction = objects.prediction { sts("stop3", trip3.id, 3) }

        val result =
            UpcomingTrip.tripsFromData(
                objects.schedules.values.toList(),
                objects.predictions.values.toList(),
                objects.trips,
                objects.vehicles
            )

        assertEquals(
            listOf(
                UpcomingTrip(trip1, trip1Schedule, trip1Prediction),
                UpcomingTrip(trip2, trip2Schedule),
                UpcomingTrip(trip3, trip3Prediction)
            ),
            result
        )
    }

    @Test
    fun `time uses schedule time if there's no prediction`() {
        val now = Clock.System.now()
        val trip = trip()

        val scheduleNormal = schedule {
            arrivalTime = now + 2.minutes
            departureTime = now + 5.minutes
        }
        val scheduleArrivalOnly = schedule {
            arrivalTime = now + 3.minutes
            departureTime = null
        }
        val scheduleDepartureOnly = schedule {
            arrivalTime = null
            departureTime = now + 4.minutes
        }

        assertEquals(scheduleNormal.arrivalTime, UpcomingTrip(trip, scheduleNormal).time)
        assertEquals(scheduleArrivalOnly.arrivalTime, UpcomingTrip(trip, scheduleArrivalOnly).time)
        assertEquals(
            scheduleDepartureOnly.departureTime,
            UpcomingTrip(trip, scheduleDepartureOnly).time
        )
    }

    @Test
    fun `time uses prediction time whether there's a schedule or not`() {
        val now = Clock.System.now()
        val trip = trip()

        val schedule = schedule {
            arrivalTime = now + 2.minutes
            departureTime = now + 5.minutes
        }
        val predictionNormal = prediction {
            arrivalTime = now + 2.5.minutes
            departureTime = now + 4.5.minutes
        }
        val predictionArrivalOnly = prediction {
            arrivalTime = now + 1.8.minutes
            departureTime = null
        }
        val predictionDepartureOnly = prediction {
            arrivalTime = null
            departureTime = now + 4.2.minutes
        }

        assertEquals(predictionNormal.arrivalTime, UpcomingTrip(trip, predictionNormal).time)
        assertEquals(
            predictionNormal.arrivalTime,
            UpcomingTrip(trip, schedule, predictionNormal).time
        )

        assertEquals(
            predictionArrivalOnly.arrivalTime,
            UpcomingTrip(trip, predictionArrivalOnly).time
        )
        assertEquals(
            predictionArrivalOnly.arrivalTime,
            UpcomingTrip(trip, schedule, predictionArrivalOnly).time
        )

        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(trip, predictionDepartureOnly).time
        )
        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(trip, schedule, predictionDepartureOnly).time
        )
    }

    @Test
    fun `time is null if prediction overrides schedule`() {
        val now = Clock.System.now()
        val trip = trip()

        val schedule = schedule {
            arrivalTime = now + 2.minutes
            departureTime = now + 5.minutes
        }
        val predictionDropped = prediction {
            arrivalTime = null
            departureTime = null
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }

        assertEquals(null, UpcomingTrip(trip, schedule, predictionDropped).time)
    }

    @Test
    fun `isArrivalOnly handles schedule without prediction`() {
        val trip = trip()

        val schedule1 = schedule {
            departureTime = null
            pickUpType = Schedule.StopEdgeType.Unavailable
        }
        assertEquals(true, UpcomingTrip(trip, schedule1).isArrivalOnly())

        val schedule2 = schedule {
            departureTime = Clock.System.now()
            pickUpType = Schedule.StopEdgeType.Regular
        }
        assertEquals(false, UpcomingTrip(trip, schedule2).isArrivalOnly())

        val schedule3 = schedule {
            pickUpType = Schedule.StopEdgeType.Unavailable
            dropOffType = Schedule.StopEdgeType.Unavailable
        }
        assertEquals(null, UpcomingTrip(trip, schedule3).isArrivalOnly())
    }

    @Test
    fun `isArrivalOnly handles prediction without schedule`() {
        val trip = trip()

        val prediction1 = prediction {
            arrivalTime = Clock.System.now()
            departureTime = null
        }
        assertEquals(true, UpcomingTrip(trip, prediction1).isArrivalOnly())

        val prediction2 = prediction {
            arrivalTime = null
            departureTime = null
        }
        assertEquals(null, UpcomingTrip(trip, prediction2).isArrivalOnly())

        val prediction3 = prediction { departureTime = Clock.System.now() }
        assertEquals(false, UpcomingTrip(trip, prediction3).isArrivalOnly())
    }

    @Test
    fun `isArrivalOnly handles schedule alongside prediction`() {
        val trip = trip()
        val scheduleArrivalOnly = schedule {
            arrivalTime = Clock.System.now()
            dropOffType = Schedule.StopEdgeType.Regular
            departureTime = null
            pickUpType = Schedule.StopEdgeType.Unavailable
        }
        val scheduleNormal = schedule {
            departureTime = Clock.System.now()
            pickUpType = Schedule.StopEdgeType.Regular
        }
        val scheduleNeither = schedule {
            dropOffType = Schedule.StopEdgeType.Unavailable
            pickUpType = Schedule.StopEdgeType.Unavailable
        }
        val predictionArrivalOnly = prediction {
            arrivalTime = Clock.System.now()
            departureTime = null
        }
        val predictionNormal = prediction { departureTime = Clock.System.now() }
        val predictionNeither = prediction {
            arrivalTime = null
            departureTime = null
        }

        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionArrivalOnly).isArrivalOnly()
        )
        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionNormal).isArrivalOnly()
        )
        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionNeither).isArrivalOnly()
        )
        assertEquals(
            false,
            UpcomingTrip(trip, scheduleNormal, predictionArrivalOnly).isArrivalOnly()
        )
        assertEquals(false, UpcomingTrip(trip, scheduleNormal, predictionNormal).isArrivalOnly())
        assertEquals(false, UpcomingTrip(trip, scheduleNormal, predictionNeither).isArrivalOnly())
        assertEquals(
            null,
            UpcomingTrip(trip, scheduleNeither, predictionArrivalOnly).isArrivalOnly()
        )
        assertEquals(null, UpcomingTrip(trip, scheduleNeither, predictionNormal).isArrivalOnly())
        assertEquals(null, UpcomingTrip(trip, scheduleNeither, predictionNeither).isArrivalOnly())
    }
}
