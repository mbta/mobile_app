package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.schedule
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
            assertEquals(
                Format.Hidden,
                UpcomingTrip(schedule { departureTime = null }).format(Clock.System.now())
            )
        }

        @Test
        fun `schedule instead of prediction`() {
            val now = Clock.System.now()
            assertEquals(
                Format.Schedule(now + 15.minutes),
                UpcomingTrip(schedule { departureTime = now + 15.minutes }).format(now)
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
        val trip1Schedule = objects.schedule { sts("stop1", "trip1", 1) }
        val trip1Prediction = objects.prediction { sts("stop1", "trip1", 1) }

        // schedule, no prediction
        val trip2Schedule = objects.schedule { sts("stop2", "trip2", 2) }

        // prediction, no schedule
        val trip3Prediction = objects.prediction { sts("stop3", "trip3", 3) }

        val result =
            UpcomingTrip.tripsFromData(
                objects.schedules.values.toList(),
                objects.predictions.values.toList(),
                objects.vehicles
            )

        assertEquals(
            listOf(
                UpcomingTrip(trip1Schedule, trip1Prediction, null),
                UpcomingTrip(trip2Schedule),
                UpcomingTrip(trip3Prediction)
            ),
            result
        )
    }

    @Test
    fun `time uses schedule time if there's no prediction`() {
        val now = Clock.System.now()

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

        assertEquals(scheduleNormal.arrivalTime, UpcomingTrip(scheduleNormal).time)
        assertEquals(scheduleArrivalOnly.arrivalTime, UpcomingTrip(scheduleArrivalOnly).time)
        assertEquals(scheduleDepartureOnly.departureTime, UpcomingTrip(scheduleDepartureOnly).time)
    }

    @Test
    fun `time uses prediction time whether there's a schedule or not`() {
        val now = Clock.System.now()

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

        assertEquals(predictionNormal.arrivalTime, UpcomingTrip(predictionNormal).time)
        assertEquals(predictionNormal.arrivalTime, UpcomingTrip(schedule, predictionNormal).time)

        assertEquals(predictionArrivalOnly.arrivalTime, UpcomingTrip(predictionArrivalOnly).time)
        assertEquals(
            predictionArrivalOnly.arrivalTime,
            UpcomingTrip(schedule, predictionArrivalOnly).time
        )

        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(predictionDepartureOnly).time
        )
        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(schedule, predictionDepartureOnly).time
        )
    }

    @Test
    fun `time is null if prediction overrides schedule`() {
        val now = Clock.System.now()

        val schedule = schedule {
            arrivalTime = now + 2.minutes
            departureTime = now + 5.minutes
        }
        val predictionDropped = prediction {
            arrivalTime = null
            departureTime = null
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }

        assertEquals(null, UpcomingTrip(schedule, predictionDropped).time)
    }
}
