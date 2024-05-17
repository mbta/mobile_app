package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant

class TripDetailsStopListTest {
    // Every good test case deserves its own DSL, right?
    private class TestBuilder {
        val objects = ObjectCollectionBuilder()

        private val childStopId = Regex("""(?<parentStop>[A-Za-z]+)\d+""")

        // Generate stops dynamically based on ID, using a numeric suffix to indicate children.
        fun stop(stopId: String): Stop {
            objects.stops[stopId]?.let {
                return it
            }

            val parentStationId =
                when (val match = childStopId.matchEntire(stopId)) {
                    null -> null
                    else -> stop(match.groups["parentStop"]!!.value).id
                }
            return objects.stop {
                id = stopId
                this.parentStationId = parentStationId
            }
        }

        fun schedule(stopId: String, stopSequence: Int) =
            objects.schedule {
                this.stopId = stop(stopId).id
                this.stopSequence = stopSequence
            }

        fun prediction(stopId: String, stopSequence: Int) =
            objects.prediction {
                this.stopId = stop(stopId).id
                this.stopSequence = stopSequence
            }

        fun stopListOf(vararg stops: TripDetailsStopList.Entry) =
            TripDetailsStopList(stops.asList())

        fun entry(
            stopId: String,
            stopSequence: Int,
            schedule: Schedule? = null,
            prediction: Prediction? = null,
            vehicle: Vehicle? = null
        ) = TripDetailsStopList.Entry(stop(stopId), stopSequence, schedule, prediction, vehicle)

        fun fromPieces(
            tripSchedules: TripSchedulesResponse?,
            tripPredictions: PredictionsStreamDataResponse?
        ) =
            TripDetailsStopList.fromPieces(
                tripSchedules,
                tripPredictions,
                GlobalResponse(objects, emptyMap())
            )

        fun schedulesResponseOf(vararg schedules: Schedule) =
            TripSchedulesResponse.Schedules(schedules.asList())

        fun schedulesResponseOf(vararg stopIds: String) =
            TripSchedulesResponse.StopIds(stopIds.asList())

        fun predictions() = PredictionsStreamDataResponse(objects)
    }

    private fun test(block: TestBuilder.() -> Unit) {
        val builder = TestBuilder()
        builder.block()
    }

    @Test
    fun `test harness generates parent stops automatically`() = test {
        assertEquals("A", stop("A1").parentStationId)
    }

    @Test fun `fromPieces returns null with no data`() = test { assertNull(fromPieces(null, null)) }

    @Test
    fun `fromPieces preserves schedules with real schedules and no predictions`() = test {
        val sched1 = schedule("A", 10)
        val sched2 = schedule("B", 20)
        val sched3 = schedule("C", 30)
        assertEquals(
            stopListOf(
                entry("A", 10, schedule = sched1),
                entry("B", 20, schedule = sched2),
                entry("C", 30, schedule = sched3)
            ),
            fromPieces(schedulesResponseOf(sched1, sched2, sched3), null)
        )
    }

    @Test
    fun `fromPieces fabricates sequence with scheduled IDs and no predictions`() = test {
        assertEquals(
            stopListOf(entry("A", 997), entry("B", 998), entry("C", 999)),
            fromPieces(schedulesResponseOf("A", "B", "C"), null)
        )
    }

    @Test
    fun `fromPieces returns null with unavailable schedules and no predictions`() = test {
        assertNull(fromPieces(TripSchedulesResponse.Unknown, null))
    }

    @Test
    fun `fromPieces preserves predictions with no schedules`() = test {
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, prediction = pred2),
                entry("C", 30, prediction = pred3)
            ),
            fromPieces(null, predictions())
        )
    }

    @Test
    fun `fromPieces matches full set of predictions to full set of schedules`() = test {
        val sched1 = schedule("A", 10)
        val sched2 = schedule("B", 20)
        val sched3 = schedule("C", 30)
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        assertEquals(
            stopListOf(
                entry("A", 10, schedule = sched1, prediction = pred1),
                entry("B", 20, schedule = sched2, prediction = pred2),
                entry("C", 30, schedule = sched3, prediction = pred3)
            ),
            fromPieces(schedulesResponseOf(sched1, sched2, sched3), predictions())
        )
    }

    @Test
    fun `fromPieces aligns scheduled stop IDs with existing predictions`() = test {
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, prediction = pred2),
                entry("C", 30, prediction = pred3)
            ),
            fromPieces(schedulesResponseOf("A", "B", "C"), predictions())
        )
    }

    @Test
    fun `fromPieces extrapolates stop sequences before current predictions`() = test {
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        assertEquals(
            stopListOf(
                entry("A", 10),
                entry("B", 20, prediction = pred2),
                entry("C", 30, prediction = pred3)
            ),
            fromPieces(schedulesResponseOf("A", "B", "C"), predictions())
        )
    }

    @Test
    fun `fromPieces extrapolates stop sequences after current predictions`() = test {
        // this case is rare
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, prediction = pred2),
                entry("C", 30)
            ),
            fromPieces(schedulesResponseOf("A", "B", "C"), predictions())
        )
    }

    @Test
    fun `fromPieces accepts sibling stops with full schedules`() = test {
        val sched1 = schedule("A1", 10)
        val sched2 = schedule("B1", 20)
        val sched3 = schedule("C1", 30)
        val pred1 = prediction("A2", 10)
        val pred2 = prediction("B2", 20)
        val pred3 = prediction("C2", 30)
        assertEquals(
            stopListOf(
                entry("A2", 10, schedule = sched1, prediction = pred1),
                entry("B2", 20, schedule = sched2, prediction = pred2),
                entry("C2", 30, schedule = sched3, prediction = pred3)
            ),
            fromPieces(schedulesResponseOf(sched1, sched2, sched3), predictions())
        )
    }

    @Test
    fun `fromPieces accepts sibling stops with stop IDs`() = test {
        stop("A1")
        stop("B1")
        stop("C1")
        val pred1 = prediction("A2", 10)
        val pred2 = prediction("B2", 20)
        val pred3 = prediction("C2", 30)
        assertEquals(
            stopListOf(
                entry("A2", 10, prediction = pred1),
                entry("B2", 20, prediction = pred2),
                entry("C2", 30, prediction = pred3)
            ),
            fromPieces(schedulesResponseOf("A1", "B1", "C1"), predictions())
        )
    }

    @Test
    fun `fromPieces resolves duplicate predictions towards schedule`() = test {
        // this case is rare
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3a = prediction("C1", 30)
        val pred3b = prediction("C2", 30)
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, prediction = pred2),
                entry("C1", 30, prediction = pred3a)
            ),
            fromPieces(schedulesResponseOf("A", "B", "C1"), predictions())
        )
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, prediction = pred2),
                entry("C2", 30, prediction = pred3b)
            ),
            fromPieces(schedulesResponseOf("A", "B", "C2"), predictions())
        )
    }

    @Test
    fun `fromPieces can deduplicate predictions by stop sequence from real data`() {
        val objects = ObjectCollectionBuilder()

        val p1 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:31:05Z")
                departureTime = Instant.parse("2024-05-10T15:32:12Z")
                stopSequence = 600
                stopId = "70200"
            }
        val p2 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:31:08Z")
                departureTime = null
                stopSequence = 600
                stopId = "71199"
            }
        val p3 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:33:07Z")
                departureTime = null
                stopSequence = 610
                stopId = "70201"
            }

        val predictions = PredictionsStreamDataResponse(objects)

        val schedules = TripSchedulesResponse.StopIds(listOf("70158", "70200", "70201"))

        val boylston =
            objects.stop {
                id = "70158"
                parentStationId = "place-boyls"
            }
        val parkStreet =
            objects.stop {
                id = "70200"
                parentStationId = "place-pktrm"
            }
        val governmentCenter =
            objects.stop {
                id = "70201"
                name = "Government Center"
                parentStationId = "place-gover"
            }
        val notQuiteParkStreet =
            objects.stop {
                id = "71199"
                description = "Park Street - Green Line - Drop-off Only"
                name = "Park Street"
                parentStationId = "place-pktrm"
            }

        val globalData = GlobalResponse(objects, emptyMap())

        val list = TripDetailsStopList.fromPieces(schedules, predictions, globalData)

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(boylston, 590, null, null, null),
                    TripDetailsStopList.Entry(parkStreet, 600, null, p1, null),
                    TripDetailsStopList.Entry(governmentCenter, 610, null, p3, null),
                )
            ),
            list
        )
    }

    @Test
    fun `fromPieces handles happy path with schedules and vehicles`() {
        val objects = ObjectCollectionBuilder()

        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }

        val stop1 = objects.stop()
        val schedule1 =
            objects.schedule {
                stopId = stop1.id
                stopSequence = 1
            }
        val prediction1 = objects.prediction(schedule1) { vehicleId = vehicle.id }

        val stop2 = objects.stop()
        val schedule2 =
            objects.schedule {
                stopId = stop2.id
                stopSequence = 2
            }
        val prediction2 = objects.prediction(schedule2) { vehicleId = vehicle.id }

        val stop3 = objects.stop()
        val schedule3 =
            objects.schedule {
                stopId = stop3.id
                stopSequence = 3
            }
        val prediction3 = objects.prediction(schedule3) { vehicleId = vehicle.id }

        val schedules = TripSchedulesResponse.Schedules(listOf(schedule1, schedule2, schedule3))
        val predictions = PredictionsStreamDataResponse(objects)
        val globalData = GlobalResponse(objects, emptyMap())

        val list = TripDetailsStopList.fromPieces(schedules, predictions, globalData)

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(stop1, 1, schedule1, prediction1, vehicle),
                    TripDetailsStopList.Entry(stop2, 2, schedule2, prediction2, vehicle),
                    TripDetailsStopList.Entry(stop3, 3, schedule3, prediction3, vehicle)
                )
            ),
            list
        )
    }
}
