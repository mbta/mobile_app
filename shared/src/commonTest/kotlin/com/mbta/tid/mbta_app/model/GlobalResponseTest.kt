package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class GlobalResponseTest {
    @Test
    fun `withRealtimeAlertsByStop properly maps alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "1" }
        val route = objects.route()
        val routePattern = objects.routePattern(route) { id = "rp1" }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, listOf(routePattern.id)))
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        val alertingStop = alertsByStop?.get(stop.id)
        assertNotNull(alertingStop)
        assertEquals(listOf(alert), alertingStop.serviceAlerts)
        assertEquals(StopServiceStatus.NO_SERVICE, alertingStop.serviceStatus)
    }

    @Test
    fun `withRealtimeAlertsByStop doesn't include stops with no alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "1" }
        val route = objects.route()
        val routePattern = objects.routePattern(route) { id = "rp1" }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        objects.alert {
            activePeriod(
                Instant.parse("2024-03-18T04:30:00-04:00"),
                Instant.parse("2024-03-22T02:30:00-04:00")
            )
            effect = Alert.Effect.Suspension
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride
                ),
                route = route.id,
                routeType = route.type,
                stop = "different stop"
            )
        }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, listOf(routePattern.id)))
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        assertNull(alertsByStop?.get(stop.id))
    }

    @Test
    fun `withRealtimeAlertsByStop properly maps child alerts`() {
        val objects = ObjectCollectionBuilder()
        val parentStop =
            objects.stop {
                id = "parent"
                childStopIds = listOf("child")
            }
        val childStop =
            objects.stop {
                id = "child"
                parentStationId = parentStop.id
            }
        val route = objects.route { id = "route" }
        val routePattern = objects.routePattern(route) { id = "rp1" }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop.id
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(childStop.id, listOf(routePattern.id)))
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertEquals(StopServiceStatus.NO_SERVICE, alertingParent.serviceStatus)

        val childAlert = alertingParent.childAlerts[childStop.id]
        assertNotNull(childAlert)
        assertEquals(StopServiceStatus.NO_SERVICE, childAlert.serviceStatus)
        assertEquals(listOf(alert), childAlert.serviceAlerts)
    }

    @Test
    fun `withRealtimeAlertsByStop properly maps partially disrupted service`() {
        val objects = ObjectCollectionBuilder()
        val parentStop =
            objects.stop {
                id = "parent"
                childStopIds = listOf("child1", "child2")
            }
        val childStop1 =
            objects.stop {
                id = "child1"
                parentStationId = parentStop.id
            }
        val childStop2 =
            objects.stop {
                id = "child2"
                parentStationId = parentStop.id
            }
        val route = objects.route { id = "route" }
        val routePattern1 = objects.routePattern(route) { id = "rp1" }
        val routePattern2 = objects.routePattern(route) { id = "rp2" }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert1 =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop1.id
                )
            }
        val alert2 =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop2.id
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop =
                    mapOf(
                        Pair(childStop1.id, listOf(routePattern1.id)),
                        Pair(childStop2.id, listOf(routePattern2.id))
                    )
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertEquals(StopServiceStatus.PARTIAL_SERVICE, alertingParent.serviceStatus)

        val child1Alert = alertingParent.childAlerts[childStop1.id]
        assertNotNull(child1Alert)
        assertEquals(StopServiceStatus.NO_SERVICE, child1Alert.serviceStatus)
        assertEquals(listOf(alert1), child1Alert.serviceAlerts)

        val child2Alert = alertingParent.childAlerts[childStop2.id]
        assertNotNull(child2Alert)
        assertEquals(StopServiceStatus.NORMAL, child2Alert.serviceStatus)
        assertEquals(emptyList(), child2Alert.serviceAlerts)
        assertEquals(listOf(alert2), child2Alert.relevantAlerts)
    }
}
