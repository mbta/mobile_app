package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.schedule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.vehicle
import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UpcomingTripTest {
    class DisplayTest {
        // trip details doesn't use UpcomingTrip
        private fun ParametricTest.anyContext() =
            anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)

        private fun ParametricTest.subway() =
            anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY, RouteType.BUS)

        @Test
        fun `status is non-null`() = parametricTest {
            assertEquals(
                TripInstantDisplay.Overridden("Custom Text"),
                UpcomingTrip(trip {}, prediction { status = "Custom Text" })
                    .display(EasternTimeInstant.now(), subway(), anyContext()),
            )
        }

        @Test
        fun `scheduled trip skipped`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Skipped(now + 15.minutes),
                UpcomingTrip(
                        trip {},
                        schedule { departureTime = now + 15.minutes },
                        prediction {
                            scheduleRelationship = Prediction.ScheduleRelationship.Skipped
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `unscheduled trip skipped`() = parametricTest {
            assertEquals(
                TripInstantDisplay.Hidden,
                UpcomingTrip(
                        trip {},
                        prediction {
                            scheduleRelationship = Prediction.ScheduleRelationship.Skipped
                        },
                    )
                    .display(EasternTimeInstant.now(), subway(), anyContext()),
            )
        }

        @Test
        fun `departure_time is null`() = parametricTest {
            assertEquals(
                TripInstantDisplay.Hidden,
                UpcomingTrip(trip {}, prediction { departureTime = null })
                    .display(EasternTimeInstant.now(), subway(), anyContext()),
            )
            assertEquals(
                TripInstantDisplay.Hidden,
                UpcomingTrip(trip {}, schedule { departureTime = null })
                    .display(EasternTimeInstant.now(), subway(), anyContext()),
            )
        }

        @Test
        fun `schedule instead of prediction`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.ScheduleMinutes(15),
                UpcomingTrip(trip {}, schedule { departureTime = now + 15.minutes })
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `departure_time in the past`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Hidden,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = null
                            departureTime = now.minus(2.seconds)
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `bus arriving now`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Now,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now
                            departureTime = now.plus(10.seconds)
                        },
                    )
                    .display(now, RouteType.BUS, anyContext()),
            )
        }

        @Test
        fun `bus more than 30 seconds away`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Minutes(2),
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.plus(2.minutes)
                            departureTime = now.plus(3.minutes)
                        },
                    )
                    .display(now, RouteType.BUS, anyContext()),
            )
        }

        @Test
        fun `seconds less than 0`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Arriving,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.minus(2.seconds)
                            departureTime = now.plus(10.seconds)
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun boarding() = parametricTest {
            val now = EasternTimeInstant.now()
            val vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip1"
            }
            assertEquals(
                TripInstantDisplay.Boarding,
                UpcomingTrip(
                        trip {},
                        prediction =
                            prediction {
                                departureTime = now.plus(10.seconds)
                                stopId = "12345"
                                tripId = "trip1"
                                vehicleId = vehicle.id
                            },
                        vehicle = vehicle,
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `not boarding when stopped at stop but more than 90 seconds until departure`() =
            parametricTest {
                val now = EasternTimeInstant.now()
                val vehicle = vehicle {
                    currentStatus = Vehicle.CurrentStatus.StoppedAt
                    stopId = "12345"
                    tripId = "trip1"
                }
                assertEquals(
                    TripInstantDisplay.Minutes(2),
                    UpcomingTrip(
                            trip {},
                            prediction =
                                prediction {
                                    departureTime = now.plus(95.seconds)
                                    stopId = "12345"
                                    tripId = "trip1"
                                    vehicleId = vehicle.id
                                },
                            vehicle = vehicle,
                        )
                        .display(now, subway(), anyContext()),
                )
            }

        @Test
        fun `not boarding`() = parametricTest {
            val now = EasternTimeInstant.now()
            // wrong vehicle status
            var vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                stopId = "12345"
                tripId = "trip1"
            }
            assertNotEquals(
                TripInstantDisplay.Boarding,
                UpcomingTrip(
                        trip {},
                        prediction =
                            prediction {
                                departureTime = now.plus(10.seconds)
                                stopId = "12345"
                                tripId = "trip1"
                                vehicleId = vehicle.id
                            },
                        vehicle = vehicle,
                    )
                    .display(now, subway(), anyContext()),
            )
            // wrong stop ID
            vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "67890"
                tripId = "trip1"
            }
            assertNotEquals(
                TripInstantDisplay.Boarding,
                UpcomingTrip(
                        trip {},
                        prediction =
                            prediction {
                                departureTime = now.plus(10.seconds)
                                stopId = "12345"
                                tripId = "trip1"
                                vehicleId = vehicle.id
                            },
                        vehicle = vehicle,
                    )
                    .display(now, subway(), anyContext()),
            )
            // wrong trip ID
            vehicle = vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = "12345"
                tripId = "trip2"
            }
            assertNotEquals(
                TripInstantDisplay.Boarding,
                UpcomingTrip(
                        trip {},
                        prediction =
                            prediction {
                                departureTime = now.plus(10.seconds)
                                stopId = "12345"
                                tripId = "trip1"
                                vehicleId = vehicle.id
                            },
                        vehicle = vehicle,
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `seconds less than 30`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Arriving,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.plus(10.seconds)
                            departureTime = now.plus(20.seconds)
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
            assertEquals(
                TripInstantDisplay.Arriving,
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(15.seconds) })
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `seconds less than 60`() = parametricTest {
            val now = EasternTimeInstant.now()
            assertEquals(
                TripInstantDisplay.Approaching,
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.plus(45.seconds)
                            departureTime = now.plus(50.seconds)
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
            assertEquals(
                TripInstantDisplay.Approaching,
                UpcomingTrip(trip {}, (prediction { departureTime = now.plus(40.seconds) }))
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `minutes in the distant future`() = parametricTest {
            val now = EasternTimeInstant.now()
            val futureMinutes = 61
            val moreFutureMinutes = futureMinutes + 38

            assertEquals(
                TripInstantDisplay.Minutes(futureMinutes),
                UpcomingTrip(
                        trip {},
                        prediction {
                            arrivalTime = now.plus(futureMinutes.minutes)
                            departureTime = now.plus(futureMinutes.minutes).plus(1.minutes)
                        },
                    )
                    .display(now, subway(), anyContext()),
            )
            assertEquals(
                TripInstantDisplay.Minutes(moreFutureMinutes),
                UpcomingTrip(
                        trip {},
                        prediction { departureTime = now.plus(moreFutureMinutes.minutes) },
                    )
                    .display(now, subway(), anyContext()),
            )
        }

        @Test
        fun `minutes less than 20`() = parametricTest {
            val now = EasternTimeInstant.now()
            val routeType = subway()
            val context = anyContext()
            assertEquals(
                TripInstantDisplay.Minutes(1),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(89.seconds) })
                    .display(now, routeType, context),
            )
            assertEquals(
                TripInstantDisplay.Minutes(2),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(90.seconds) })
                    .display(now, routeType, context),
            )
            assertEquals(
                TripInstantDisplay.Minutes(2),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(149.seconds) })
                    .display(now, routeType, context),
            )
            assertEquals(
                TripInstantDisplay.Minutes(3),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(150.seconds) })
                    .display(now, routeType, context),
            )
            assertEquals(
                TripInstantDisplay.Minutes(3),
                UpcomingTrip(trip {}, prediction { departureTime = now.plus(209.seconds) })
                    .display(now, routeType, context),
            )
            assertEquals(
                TripInstantDisplay.Minutes(45),
                UpcomingTrip(trip {}, (prediction { departureTime = now.plus(45.minutes) }))
                    .display(now, routeType, context),
            )
        }
    }

    @Test
    fun `tripsFromData matches things up`() {
        fun ObjectCollectionBuilder.ScheduleBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int,
        ) {
            this.stopId = stopId
            this.tripId = tripId
            this.stopSequence = stopSequence
        }
        fun ObjectCollectionBuilder.PredictionBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int,
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
                objects.stops,
                objects.schedules.values.toList(),
                objects.predictions.values.toList(),
                objects.trips,
                objects.vehicles,
                EasternTimeInstant.now(),
            )

        assertEquals(
            listOf(
                UpcomingTrip(trip1, trip1Schedule, trip1Prediction),
                UpcomingTrip(trip2, trip2Schedule),
                UpcomingTrip(trip3, trip3Prediction),
            ),
            result,
        )
    }

    @Test
    fun `tripsFromData ignores stop sequence on cancelled trips`() {
        fun ObjectCollectionBuilder.ScheduleBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int,
        ) {
            this.stopId = stopId
            this.tripId = tripId
            this.stopSequence = stopSequence
        }
        fun ObjectCollectionBuilder.PredictionBuilder.sts(
            stopId: String,
            tripId: String,
            stopSequence: Int,
        ) {
            this.stopId = stopId
            this.tripId = tripId
            this.stopSequence = stopSequence
        }

        val objects = ObjectCollectionBuilder()

        // schedule and prediction match
        val trip = objects.trip()
        val schedule = objects.schedule { sts("stop1", trip.id, 1) }
        val prediction =
            objects.prediction {
                sts("stop1", trip.id, 2)
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }

        val result =
            UpcomingTrip.tripsFromData(
                objects.stops,
                objects.schedules.values.toList(),
                objects.predictions.values.toList(),
                objects.trips,
                objects.vehicles,
                EasternTimeInstant.now(),
            )

        assertEquals(listOf(UpcomingTrip(trip, schedule, prediction)), result)
    }

    @Test
    fun `time uses schedule time if there's no prediction`() {
        val now = EasternTimeInstant.now()
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
            UpcomingTrip(trip, scheduleDepartureOnly).time,
        )
    }

    @Test
    fun `time uses prediction time whether there's a schedule or not`() {
        val now = EasternTimeInstant.now()
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
            UpcomingTrip(trip, schedule, predictionNormal).time,
        )

        assertEquals(
            predictionArrivalOnly.arrivalTime,
            UpcomingTrip(trip, predictionArrivalOnly).time,
        )
        assertEquals(
            predictionArrivalOnly.arrivalTime,
            UpcomingTrip(trip, schedule, predictionArrivalOnly).time,
        )

        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(trip, predictionDepartureOnly).time,
        )
        assertEquals(
            predictionDepartureOnly.departureTime,
            UpcomingTrip(trip, schedule, predictionDepartureOnly).time,
        )
    }

    @Test
    fun `time is null if prediction overrides schedule`() {
        val now = EasternTimeInstant.now()
        val trip = trip()

        val schedule = schedule {
            arrivalTime = now + 2.minutes
            departureTime = now + 5.minutes
        }
        val predictionDropped = prediction {
            arrivalTime = null
            departureTime = null
            scheduleRelationship = Prediction.ScheduleRelationship.Skipped
        }

        assertEquals(null, UpcomingTrip(trip, schedule, predictionDropped).time)
    }

    @Test
    fun `isArrivalOnly handles schedule without prediction`() {
        val trip = trip()

        val schedule1 = schedule {
            arrivalTime = EasternTimeInstant.now()
            departureTime = null
        }
        assertEquals(true, UpcomingTrip(trip, schedule1).isArrivalOnly())

        val schedule2 = schedule {
            arrivalTime = EasternTimeInstant.now()
            departureTime = EasternTimeInstant.now()
        }
        assertEquals(false, UpcomingTrip(trip, schedule2).isArrivalOnly())

        val schedule3 = schedule {
            arrivalTime = null
            departureTime = null
        }
        assertEquals(null, UpcomingTrip(trip, schedule3).isArrivalOnly())
    }

    @Test
    fun `isArrivalOnly handles prediction without schedule`() {
        val trip = trip()

        val prediction1 = prediction {
            arrivalTime = EasternTimeInstant.now()
            departureTime = null
        }
        assertEquals(true, UpcomingTrip(trip, prediction1).isArrivalOnly())

        val prediction2 = prediction {
            arrivalTime = null
            departureTime = null
        }
        assertEquals(null, UpcomingTrip(trip, prediction2).isArrivalOnly())

        val prediction3 = prediction { departureTime = EasternTimeInstant.now() }
        assertEquals(false, UpcomingTrip(trip, prediction3).isArrivalOnly())
    }

    @Test
    fun `isArrivalOnly handles schedule alongside prediction`() {
        val trip = trip()
        val scheduleArrivalOnly = schedule {
            arrivalTime = EasternTimeInstant.now()
            departureTime = null
        }
        val scheduleNormal = schedule { departureTime = EasternTimeInstant.now() }
        val scheduleNeither = schedule {
            arrivalTime = null
            departureTime = null
        }
        val predictionArrivalOnly = prediction {
            arrivalTime = EasternTimeInstant.now()
            departureTime = null
        }
        val predictionNormal = prediction { departureTime = EasternTimeInstant.now() }
        val predictionNeither = prediction {
            arrivalTime = null
            departureTime = null
        }

        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionArrivalOnly).isArrivalOnly(),
        )
        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionNormal).isArrivalOnly(),
        )
        assertEquals(
            true,
            UpcomingTrip(trip, scheduleArrivalOnly, predictionNeither).isArrivalOnly(),
        )
        assertEquals(
            false,
            UpcomingTrip(trip, scheduleNormal, predictionArrivalOnly).isArrivalOnly(),
        )
        assertEquals(false, UpcomingTrip(trip, scheduleNormal, predictionNormal).isArrivalOnly())
        assertEquals(false, UpcomingTrip(trip, scheduleNormal, predictionNeither).isArrivalOnly())
        assertEquals(
            null,
            UpcomingTrip(trip, scheduleNeither, predictionArrivalOnly).isArrivalOnly(),
        )
        assertEquals(null, UpcomingTrip(trip, scheduleNeither, predictionNormal).isArrivalOnly())
        assertEquals(null, UpcomingTrip(trip, scheduleNeither, predictionNeither).isArrivalOnly())
    }

    @Test
    fun `stopSequence works if prediction and schedule both exist`() {
        val trip = trip()

        val schedule1 = schedule { stopSequence = 1 }
        val schedule2 = schedule { stopSequence = 2 }
        val prediction1 = prediction { stopSequence = 1 }
        val prediction2 = prediction { stopSequence = 2 }

        assertEquals(1, UpcomingTrip(trip, schedule1, prediction1).stopSequence)
        assertEquals(1, UpcomingTrip(trip, schedule1, prediction2).stopSequence)
        assertEquals(2, UpcomingTrip(trip, schedule2, prediction1).stopSequence)
        assertEquals(2, UpcomingTrip(trip, schedule2, prediction2).stopSequence)
    }

    @Test
    fun `stopSequence works if only prediction or only schedule exists`() {
        val trip = trip()

        val schedule = schedule { stopSequence = 90 }
        val prediction = prediction { stopSequence = 120 }

        assertEquals(90, UpcomingTrip(trip, schedule).stopSequence)
        assertEquals(120, UpcomingTrip(trip, prediction).stopSequence)
    }

    @Test
    fun `stopHeadsign in schedule overrides trip headsign`() {
        val trip = trip { headsign = "Trip Headsign" }
        val schedule = schedule { stopHeadsign = "Stop Headsign" }

        assertEquals(schedule.stopHeadsign, UpcomingTrip(trip, schedule).headsign)
    }

    @Test
    fun `stopSequence returns null if neither prediction nor schedule exists`() {
        val trip = trip()

        assertNull(UpcomingTrip(trip, null, null, null).stopSequence)
    }
}
