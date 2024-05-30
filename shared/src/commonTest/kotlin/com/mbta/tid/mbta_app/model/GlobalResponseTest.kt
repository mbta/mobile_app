package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertContains
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

    @Test
    fun `mapStop data is created`() {
        val objects = ObjectCollectionBuilder()
        val stopA =
            objects.stop {
                id = "A"
                childStopIds = listOf("A1")
            }
        val stopA1 =
            objects.stop {
                id = "A1"
                parentStationId = "A"
            }
        val stopB = objects.stop { id = "B" }
        val stopC = objects.stop { id = "C" }
        val stopD =
            objects.stop {
                id = "D"
                locationType = LocationType.STOP
            }
        val stopE =
            objects.stop {
                id = "E"
                locationType = LocationType.STATION
            }

        val routeRed =
            objects.route {
                id = "Red"
                sortOrder = 1
            }
        val routeBlue =
            objects.route {
                id = "Blue"
                sortOrder = 2
            }
        val routeSilver =
            objects.route {
                id = "742"
                sortOrder = 3
            }
        val routeCR =
            objects.route {
                id = "CR"
                type = RouteType.COMMUTER_RAIL
                sortOrder = 4
            }
        val routeBus =
            objects.route {
                id = "1"
                type = RouteType.BUS
                sortOrder = 5
            }
        val routeShuttle =
            objects.route {
                id = "Shuttle-BraintreeQuincyCenter"
                type = RouteType.BUS
                sortOrder = 5
            }

        val patternRed =
            objects.routePattern(routeRed) {
                id = "R1"
                typicality = RoutePattern.Typicality.Typical
            }
        val patternBlue =
            objects.routePattern(routeBlue) {
                id = "B1"
                typicality = RoutePattern.Typicality.Atypical
            }
        val patternSilver =
            objects.routePattern(routeSilver) {
                id = "S1"
                typicality = RoutePattern.Typicality.CanonicalOnly
            }
        val patternBus =
            objects.routePattern(routeBus) {
                id = "Bus1"
                typicality = RoutePattern.Typicality.Typical
            }
        val patternCR =
            objects.routePattern(routeCR) {
                id = "CR1"
                typicality = RoutePattern.Typicality.CanonicalOnly
            }
        val patternShuttle =
            objects.routePattern(routeShuttle) {
                id = "Shuttle1"
                typicality = RoutePattern.Typicality.Typical
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop =
                    mapOf(
                        Pair(stopA.id, listOf(patternRed.id, patternShuttle.id)),
                        Pair(stopA1.id, listOf(patternRed.id, patternBlue.id, patternSilver.id)),
                        Pair(
                            stopB.id,
                            listOf(patternShuttle.id, patternCR.id, patternBus.id, patternRed.id)
                        ),
                        Pair(stopC.id, listOf(patternBus.id, patternSilver.id)),
                        Pair(stopD.id, listOf(patternSilver.id)),
                        Pair(stopE.id, listOf(patternSilver.id))
                    )
            )

        val staticData = GlobalStaticData(globalData = globalResponse)

        assertContains(
            staticData.mapStops[stopA.id]!!.routeTypes,
            MapStopRoute.RED,
            "Route type should be associated with the stop it serves"
        )
        assertContains(
            staticData.mapStops[stopA.id]!!.routeTypes,
            MapStopRoute.SILVER,
            "Silver line routes should be properly identified, and child routes included in parent"
        )
        assertContains(
            staticData.mapStops[stopA.id]!!.routes[MapStopRoute.SILVER]!!,
            routeSilver,
            "Route type should be associated with the specific route that it was identified from"
        )
        assertEquals(
            listOf(MapStopRoute.RED, MapStopRoute.COMMUTER, MapStopRoute.BUS),
            staticData.mapStops[stopB.id]!!.routeTypes,
            "Route types are ordered to match the route sort order"
        )

        assertFalse(
            staticData.mapStops[stopA1.id]!!.routeTypes.contains(MapStopRoute.BLUE),
            "Atypical routes should not be included"
        )
        assertFalse(
            staticData.mapStops[stopA.id]!!.routeTypes.contains(MapStopRoute.BUS),
            "Shuttle routes should not be included"
        )

        assertEquals(
            listOf(MapStopRoute.BUS),
            staticData.mapStops[stopC.id]!!.routeTypes,
            "If a stop contains both SL and regular bus routes, only consider it a bus stop"
        )
        assertEquals(
            listOf(MapStopRoute.BUS),
            staticData.mapStops[stopD.id]!!.routeTypes,
            "If a stop is a stop type and only has SL, only consider it a bus stop"
        )
        assertEquals(
            listOf(MapStopRoute.SILVER),
            staticData.mapStops[stopE.id]!!.routeTypes,
            "If a stop is a station type and only has SL, consider it a SL station"
        )
    }
}
