package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TripDetailsStopListTest {
    // Every good test case deserves its own DSL, right?
    private class TestBuilder {
        val objects = ObjectCollectionBuilder()

        private val childStopId = Regex("""(?<parentStop>[A-Za-z]+)\d+""")

        // Generate stops dynamically based on ID, using a numeric suffix to indicate children.
        fun stop(
            stopId: String,
            childStopIds: List<String> = listOf(),
            connectingStopIds: List<String> = listOf()
        ): Stop {
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
                this.childStopIds = childStopIds
                this.connectingStopIds = connectingStopIds
            }
        }

        private lateinit var _trip: Trip

        fun trip(block: ObjectCollectionBuilder.TripBuilder.() -> Unit): Trip {
            return objects.trip(block).also { _trip = it }
        }

        fun schedule(stopId: String, stopSequence: Int, routeId: String = "") =
            objects.schedule {
                this.stopId = stop(stopId).id
                this.stopSequence = stopSequence
                if (this@TestBuilder::_trip.isInitialized) this.trip = _trip
                this.routeId = routeId
            }

        fun prediction(
            stopId: String,
            stopSequence: Int,
            routeId: String = "",
            time: Instant? = null
        ) =
            objects.prediction {
                this.stopId = stop(stopId).id
                this.stopSequence = stopSequence
                if (this@TestBuilder::_trip.isInitialized) this.trip = _trip
                this.routeId = routeId
                this.departureTime = time
            }

        fun pattern(
            patternId: String,
            route: Route,
            typicality: RoutePattern.Typicality = RoutePattern.Typicality.Typical
        ) =
            objects.routePattern(route) {
                this.id = patternId
                this.typicality = typicality
            }

        fun alert(effect: Alert.Effect, block: ObjectCollectionBuilder.AlertBuilder.() -> Unit) =
            objects.alert {
                this.effect = effect
                this.activePeriod(Instant.DISTANT_PAST, null)
                block()
            }

        fun stopListOf(vararg stops: TripDetailsStopList.Entry, terminalStop: TripDetailsStopList.Entry? = null) =
            TripDetailsStopList(stops.asList(), terminalStop ?: stops.firstOrNull())

        fun entry(
            stopId: String,
            stopSequence: Int,
            alert: Alert? = null,
            schedule: Schedule? = null,
            prediction: Prediction? = null,
            vehicle: Vehicle? = null,
            routes: List<Route> = listOf()
        ) =
            TripDetailsStopList.Entry(
                stop(stopId),
                stopSequence,
                alert,
                schedule,
                prediction,
                vehicle,
                routes
            )

        fun globalData(patternIdsByStop: Map<String, List<String>> = emptyMap()) =
            GlobalResponse(objects, patternIdsByStop)

        fun fromPieces(
            tripSchedules: TripSchedulesResponse?,
            tripPredictions: PredictionsStreamDataResponse?,
            vehicle: Vehicle? = null,
            patternIdsByStop: Map<String, List<String>> = emptyMap(),
            trip: Trip = Trip("trip", 0, "", "")
        ) =
            TripDetailsStopList.fromPieces(
                trip.id,
                trip.directionId,
                tripSchedules,
                tripPredictions,
                vehicle,
                AlertsStreamDataResponse(objects),
                globalData(patternIdsByStop)
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

    @Test
    fun `fromPieces returns empty list with no data`() = test {
        assertEquals(TripDetailsStopList(stops = emptyList()), fromPieces(null, null))
    }

    @Test
    fun `fromPieces returns schedules when there are no predictions`() = test {
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
    fun `fromPieces returns null with scheduled IDs and no predictions`() = test {
        val sched1 = schedule("A", 10)
        val sched2 = schedule("B", 20)
        val sched3 = schedule("C", 30)
        assertEquals(
            stopListOf(entry("A", 997), entry("B", 998), entry("C", 999)),
            fromPieces(schedulesResponseOf(sched1.stopId, sched2.stopId, sched3.stopId), null)
        )
    }

    @Test
    fun `fromPieces returns null with unavailable schedules and no predictions`() = test {
        assertEquals(
            TripDetailsStopList(stops = emptyList()),
            fromPieces(TripSchedulesResponse.Unknown, null)
        )
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

        val trip = objects.trip {}

        val alertsData = AlertsStreamDataResponse(objects)
        val globalData = GlobalResponse(objects, emptyMap())

        val list =
            TripDetailsStopList.fromPieces(
                trip.id,
                trip.directionId,
                schedules,
                predictions,
                null,
                alertsData,
                globalData
            )

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(boylston, 590, null, null, null, null, listOf()),
                    TripDetailsStopList.Entry(parkStreet, 600, null, null, p1, null, listOf()),
                    TripDetailsStopList.Entry(
                        governmentCenter,
                        610,
                        null,
                        null,
                        p3,
                        null,
                        listOf()
                    ),
                ),
                TripDetailsStopList.Entry(boylston, 590, null, null, null, null, listOf())
            ),
            list
        )
    }

    @Test
    fun `fromPieces handles happy path with schedules and vehicles`() {
        val objects = ObjectCollectionBuilder()

        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        val outOfDateVehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val stop1 = objects.stop()
        val schedule1 =
            objects.schedule {
                stopId = stop1.id
                stopSequence = 1
            }
        val prediction1 = objects.prediction(schedule1) { vehicleId = outOfDateVehicle.id }

        val stop2 = objects.stop()
        val schedule2 =
            objects.schedule {
                stopId = stop2.id
                stopSequence = 2
            }
        val prediction2 = objects.prediction(schedule2) { vehicleId = outOfDateVehicle.id }

        val stop3 = objects.stop()
        val schedule3 =
            objects.schedule {
                stopId = stop3.id
                stopSequence = 3
            }
        val prediction3 = objects.prediction(schedule3) { vehicleId = outOfDateVehicle.id }

        val trip = objects.trip {}
        val schedules = TripSchedulesResponse.Schedules(listOf(schedule1, schedule2, schedule3))
        val predictions = PredictionsStreamDataResponse(objects)
        val alertsData = AlertsStreamDataResponse(objects)
        val globalData = GlobalResponse(objects, emptyMap())

        val list =
            TripDetailsStopList.fromPieces(
                trip.id,
                trip.directionId,
                schedules,
                predictions,
                vehicle,
                alertsData,
                globalData
            )

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        1,
                        null,
                        schedule1,
                        prediction1,
                        vehicle,
                        listOf()
                    ),
                    TripDetailsStopList.Entry(
                        stop2,
                        2,
                        null,
                        schedule2,
                        prediction2,
                        vehicle,
                        listOf()
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        3,
                        null,
                        schedule3,
                        prediction3,
                        vehicle,
                        listOf()
                    )
                ),
                TripDetailsStopList.Entry(
                    stop1,
                    1,
                    null,
                    schedule1,
                    prediction1,
                    vehicle,
                    listOf()
                )
            ),
            list
        )
    }

    @Test
    fun `fromPieces includes all transfer routes`() = test {
        stop("A", listOf("A1", "A2"), listOf("A3"))
        stop("B", listOf("B1"))
        val stopC = stop("C", listOf("C1"))
        val stopA1 = stop("A1")
        val stopA2 = stop("A2")
        val stopA3 = stop("A3", listOf("A4"))
        val stopA4 = stop("A4")
        val stopB1 = stop("B1")
        val stopC1 = stop("C1")

        val routeCurrent = objects.route { id = "V" }
        val routeW =
            objects.route {
                id = "W"
                sortOrder = 1
            }
        val routeX =
            objects.route {
                id = "X"
                sortOrder = 2
            }
        val routeY =
            objects.route {
                id = "Y"
                sortOrder = 3
            }
        val routeZ =
            objects.route {
                id = "Z"
                sortOrder = 4
            }
        val routeExcluded =
            objects.route {
                id = "116117"
                sortOrder = 0
            }

        val patternCurrent1 = pattern("V1", routeCurrent, RoutePattern.Typicality.Atypical)
        val patternCurrent2 = pattern("V2", routeCurrent)
        val patternW1 = pattern("W1", routeW)
        val patternW2 = pattern("W2", routeW, RoutePattern.Typicality.CanonicalOnly)
        val patternX1 = pattern("X1", routeX)
        val patternY1 = pattern("Y1", routeY)
        val patternZ1 = pattern("Z1", routeZ)
        val patternExcluded = pattern("116117", routeExcluded)

        val sched1 = schedule("A2", 10, routeCurrent.id)
        val sched2 = schedule("B1", 20, routeCurrent.id)
        val sched3 = schedule("C1", 30, routeCurrent.id)

        val pred1 = prediction("A1", 10, routeCurrent.id)
        val pred2 = prediction("B1", 20, routeCurrent.id)
        val pred3 = prediction("C1", 30, routeCurrent.id)

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                routeId = routeCurrent.id
            }

        assertEquals(
            stopListOf(
                entry(
                    "A1",
                    10,
                    vehicle = vehicle,
                    schedule = sched1,
                    prediction = pred1,
                    routes = listOf(routeW, routeX, routeY, routeZ)
                ),
                entry(
                    "B1",
                    20,
                    vehicle = vehicle,
                    schedule = sched2,
                    prediction = pred2,
                    routes = listOf(routeZ)
                ),
                entry(
                    "C1",
                    30,
                    vehicle = vehicle,
                    schedule = sched3,
                    prediction = pred3,
                    routes = listOf(routeY)
                )
            ),
            fromPieces(
                schedulesResponseOf(sched1, sched2, sched3),
                predictions(),
                vehicle,
                mapOf(
                    Pair(stopA1.id, listOf(patternCurrent1.id, patternW1.id)),
                    Pair(stopA2.id, listOf(patternZ1.id, patternExcluded.id)),
                    Pair(stopA3.id, listOf(patternX1.id)),
                    Pair(stopA4.id, listOf(patternY1.id)),
                    Pair(stopB1.id, listOf(patternZ1.id, patternCurrent2.id)),
                    Pair(stopC.id, listOf(patternW2.id, patternY1.id, patternExcluded.id)),
                    Pair(stopC1.id, listOf(patternY1.id, patternCurrent1.id, patternCurrent2.id))
                )
            )
        )
    }

    @Test
    fun `fromPieces resolves current route from available data`() = test {
        val stopA = stop("A")
        val stopB = stop("B")

        val routeCurrent = objects.route { id = "X" }
        val routeOther = objects.route { id = "Y" }

        val patternCurrent = pattern("X1", routeCurrent)
        val patternOther = pattern("Y1", routeOther)

        val sched = schedule(stopA.id, 10, routeCurrent.id)
        val pred = prediction(stopB.id, 20, routeCurrent.id)

        assertEquals(
            stopListOf(
                entry(stopA.id, stopSequence = 10, schedule = sched, routes = listOf(routeOther))
            ),
            fromPieces(
                schedulesResponseOf(sched),
                null,
                null,
                mapOf(Pair(stopA.id, listOf(patternCurrent.id, patternOther.id)))
            )
        )

        assertEquals(
            stopListOf(entry(stopB.id, 20, prediction = pred, routes = listOf(routeOther))),
            fromPieces(
                null,
                predictions(),
                null,
                mapOf(Pair(stopB.id, listOf(patternCurrent.id, patternOther.id)))
            )
        )
    }

    @Test
    fun `fromPieces discards stops vehicle has passed`() = test {
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        val trip = objects.trip {}
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                currentStopSequence = 20
                tripId = trip.id
            }
        assertEquals(
            stopListOf(
                entry("B", 20, prediction = pred2, vehicle = vehicle),
                entry("C", 30, prediction = pred3, vehicle = vehicle),
                terminalStop = entry("A", 10, prediction = pred1, vehicle = vehicle),
            ),
            fromPieces(null, predictions(), vehicle, trip = trip)
        )
    }

    @Test
    fun `fromPieces discards stops vehicle is currently at`() = test {
        val pred1 = prediction("A", 10)
        prediction("B", 20)
        val pred3 = prediction("C", 30)
        val trip = objects.trip {}
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                currentStopSequence = 20
                tripId = trip.id
            }
        assertEquals(
            stopListOf(
                entry("C", 30, prediction = pred3, vehicle = vehicle),
                terminalStop = entry("A", 10, prediction = pred1, vehicle = vehicle),
            ),
            fromPieces(null, predictions(), vehicle, trip = trip)
        )
    }

    @Test
    fun `fromPieces checks trip before discarding past stops`() = test {
        val pred1 = prediction("A", 10)
        val pred2 = prediction("B", 20)
        val pred3 = prediction("C", 30)
        val trip = objects.trip {}
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                currentStopSequence = 20
                tripId = "differentTrip"
            }
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1, vehicle = vehicle),
                entry("B", 20, prediction = pred2, vehicle = vehicle),
                entry("C", 30, prediction = pred3, vehicle = vehicle)
            ),
            fromPieces(null, predictions(), vehicle, trip = trip)
        )
    }

    @Test
    fun `fromPieces keeps alerts`() = test {
        val now = Clock.System.now()
        val pred1 = prediction("A", 10, time = now + 1.minutes)
        val pred2 = prediction("B", 20, time = now + 2.minutes)
        val pred3 = prediction("C", 30, time = now + 3.minutes)
        val alert =
            alert(Alert.Effect.Detour) {
                informedEntity(listOf(Alert.InformedEntity.Activity.Board), stop = "B")
            }
        assertEquals(
            stopListOf(
                entry("A", 10, prediction = pred1),
                entry("B", 20, alert = alert, prediction = pred2),
                entry("C", 30, prediction = pred3)
            ),
            fromPieces(null, predictions())
        )
    }

    @Test
    fun `fromPieces does not crash on multi-route trips`() = test {
        val now = Clock.System.now()
        trip { routeId = "1" }
        val pred1 = prediction("A", 10, routeId = "1", time = now + 1.minutes)
        val pred2 = prediction("A", 10, routeId = "2", time = now + 1.minutes)
        assertEquals(
            stopListOf(entry("A", 10, prediction = pred1)),
            fromPieces(null, predictions())
        )
    }

    @Test
    fun `splitForTarget distinguishes duplicate stop IDs by stop sequence`() = test {
        val list = stopListOf(entry("A", 10), entry("B", 20), entry("C", 30), entry("A", 40))

        assertEquals(
            TripDetailsStopList.TargetSplit(
                collapsedStops = emptyList(),
                targetStop = entry("A", 10),
                followingStops = listOf(entry("B", 20), entry("C", 30), entry("A", 40))
            ),
            list.splitForTarget("A", 10, globalData())
        )
        assertEquals(
            TripDetailsStopList.TargetSplit(
                collapsedStops = listOf(entry("A", 10), entry("B", 20), entry("C", 30)),
                targetStop = entry("A", 40),
                followingStops = emptyList()
            ),
            list.splitForTarget("A", 40, globalData())
        )
    }

    @Test
    fun `splitForTarget uses last copy if stop sequence not found`() = test {
        val list = stopListOf(entry("A", 996), entry("C", 997), entry("A", 998), entry("B", 999))

        assertEquals(
            TripDetailsStopList.TargetSplit(
                collapsedStops = listOf(entry("A", 996), entry("C", 997)),
                targetStop = entry("A", 998),
                followingStops = listOf(entry("B", 999))
            ),
            list.splitForTarget("A", 3, globalData())
        )
    }

    @Test
    fun `splitForTarget accepts siblings`() = test {
        val list = stopListOf(entry("A1", 10), entry("B1", 20), entry("C1", 30))
        stop("B2")

        assertEquals(
            TripDetailsStopList.TargetSplit(
                collapsedStops = listOf(entry("A1", 10)),
                targetStop = entry("B1", 20),
                followingStops = listOf(entry("C1", 30)),
            ),
            list.splitForTarget("B2", 20, globalData())
        )
    }

    @Test
    fun `splitForTarget returns null if target not found`() = test {
        val list = stopListOf(entry("A", 10), entry("B", 20), entry("C", 30))
        stop("D")

        assertNull(list.splitForTarget("D", 40, globalData()))
    }
}
