package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(alertingStop.serviceAlerts, listOf(alert))
        assertTrue(alertingStop.hasNoService)
        assertTrue(alertingStop.hasSomeDisruptedService)
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
                childStopIds = listOf("nestedChild")
                parentStationId = parentStop.id
            }
        val nestedChildStop =
            objects.stop {
                id = "nestedChild"
                parentStationId = childStop.id
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
                    stop = nestedChildStop.id
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(nestedChildStop.id, listOf(routePattern.id)))
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertTrue(alertingParent.hasNoService)
        assertTrue(alertingParent.hasSomeDisruptedService)

        val childAlert = alertingParent.childAlerts[childStop.id]
        val nestedChildAlert = childAlert?.childAlerts?.get(nestedChildStop.id)
        assertNotNull(nestedChildAlert)
        assertTrue(nestedChildAlert.hasNoService)
        assertEquals(nestedChildAlert.serviceAlerts, listOf(alert))
    }

    @Test
    fun `withRealtimeAlertsByStop properly maps partially disrupted service`() {
        val objects = ObjectCollectionBuilder()
        val parentStop =
            objects.stop {
                id = "parent"
                childStopIds = listOf("child")
            }
        val childStop =
            objects.stop {
                id = "child"
                childStopIds = listOf("nestedChild1", "nestedChild2")
                parentStationId = parentStop.id
            }
        val nestedChildStop1 =
            objects.stop {
                id = "nestedChild1"
                parentStationId = childStop.id
            }
        val nestedChildStop2 =
            objects.stop {
                id = "nestedChild2"
                parentStationId = childStop.id
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
                    stop = nestedChildStop1.id
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
                    stop = nestedChildStop2.id
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop =
                    mapOf(
                        Pair(nestedChildStop1.id, listOf(routePattern1.id)),
                        Pair(nestedChildStop2.id, listOf(routePattern2.id))
                    )
            )
        val staticData = GlobalStaticData(globalData = globalResponse)
        val alertsByStop =
            staticData.withRealtimeAlertsByStop(AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertFalse(alertingParent.hasNoService)
        assertTrue(alertingParent.hasSomeDisruptedService)

        val childAlert = alertingParent.childAlerts[childStop.id]
        assertNotNull(childAlert)

        val nestedChild1Alert = childAlert.childAlerts[nestedChildStop1.id]
        assertNotNull(nestedChild1Alert)
        assertTrue(nestedChild1Alert.hasNoService)
        assertEquals(nestedChild1Alert.serviceAlerts, listOf(alert1))

        val nestedChild2Alert = childAlert.childAlerts[nestedChildStop2.id]
        assertNotNull(nestedChild2Alert)
        assertFalse(nestedChild2Alert.hasNoService)
        assertEquals(nestedChild2Alert.serviceAlerts, emptyList())
        assertEquals(nestedChild2Alert.relevantAlerts, setOf(alert2))
    }
}
