package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.map.MapTestDataHelper.global
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.silverRoutes
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GlobalResponseTest {
    @Test
    fun `withRealtimeAlertsByStop properly maps alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "1" }
        val route = objects.route { id = "Blue" }
        val routePattern = objects.routePattern(route) { id = "rp1" }
        val routeType = MapStopRoute.matching(route)

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00"),
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id,
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, listOf(routePattern.id))),
            )
        val alertsByStop =
            GlobalMapData.getAlertsByStop(globalResponse, AlertsStreamDataResponse(objects), time)

        val alertingStop = alertsByStop?.get(stop.id)
        assertNotNull(alertingStop)
        assertEquals(listOf(alert), alertingStop.serviceAlerts)
        assertEquals(StopAlertState.Suspension, alertingStop.stateByRoute[routeType])
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
                Instant.parse("2024-03-22T02:30:00-04:00"),
            )
            effect = Alert.Effect.Suspension
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                route = route.id,
                routeType = route.type,
                stop = "different stop",
            )
        }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, listOf(routePattern.id))),
            )
        val alertsByStop =
            GlobalMapData.getAlertsByStop(globalResponse, AlertsStreamDataResponse(objects), time)

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
        val route = objects.route { id = "Red" }
        val routePattern = objects.routePattern(route) { id = "rp1" }
        val routeType = MapStopRoute.matching(route)

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00"),
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop.id,
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(childStop.id, listOf(routePattern.id))),
            )
        val alertsByStop =
            GlobalMapData.getAlertsByStop(globalResponse, AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertEquals(StopAlertState.Suspension, alertingParent.stateByRoute[routeType])

        val childAlert = alertingParent.childAlerts[childStop.id]
        assertNotNull(childAlert)
        assertEquals(StopAlertState.Suspension, childAlert.stateByRoute[routeType])
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
        val route = objects.route { id = "Orange" }
        val routePattern1 = objects.routePattern(route) { id = "rp1" }
        val routePattern2 = objects.routePattern(route) { id = "rp2" }
        val routeType = MapStopRoute.matching(route)

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert1 =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00"),
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop1.id,
                )
            }
        val alert2 =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00"),
                )
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(),
                    route = route.id,
                    routeType = route.type,
                    stop = childStop2.id,
                )
            }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop =
                    mapOf(
                        Pair(childStop1.id, listOf(routePattern1.id)),
                        Pair(childStop2.id, listOf(routePattern2.id)),
                    ),
            )
        val alertsByStop =
            GlobalMapData.getAlertsByStop(globalResponse, AlertsStreamDataResponse(objects), time)

        val alertingParent = alertsByStop?.get(parentStop.id)
        assertNotNull(alertingParent)
        assertTrue(alertingParent.relevantAlerts.isEmpty())
        assertEquals(StopAlertState.Suspension, alertingParent.stateByRoute[routeType])

        val child1Alert = alertingParent.childAlerts[childStop1.id]
        assertNotNull(child1Alert)
        assertEquals(StopAlertState.Suspension, child1Alert.stateByRoute[routeType])
        assertEquals(listOf(alert1), child1Alert.serviceAlerts)

        val child2Alert = alertingParent.childAlerts[childStop2.id]
        assertNotNull(child2Alert)
        assertEquals(StopAlertState.Normal, child2Alert.stateByRoute[routeType])
        assertEquals(emptyList(), child2Alert.serviceAlerts)
        assertEquals(listOf(alert2), child2Alert.relevantAlerts)
    }

    @Test
    fun `withRealtimeAlertsByStop properly maps boundary stops`() {
        val objects = ObjectCollectionBuilder()
        val parent1Stop =
            objects.stop {
                id = "parent1"
                childStopIds = listOf("child1")
            }
        val child1Stop =
            objects.stop {
                id = "child1"
                parentStationId = parent1Stop.id
            }
        val parent2Stop =
            objects.stop {
                id = "parent2"
                childStopIds = listOf("child2")
            }
        val child2Stop =
            objects.stop {
                id = "child2"
                parentStationId = parent2Stop.id
            }
        val parent3Stop =
            objects.stop {
                id = "parent3"
                childStopIds = listOf("child3")
            }
        val child3Stop =
            objects.stop {
                id = "child3"
                parentStationId = parent3Stop.id
            }
        val route = objects.route { id = "Blue" }
        val routePattern =
            objects.routePattern(route) {
                id = "rp1"
                typicality = RoutePattern.Typicality.Typical
                representativeTripId = "trip"
            }
        objects.trip(routePattern) {
            id = "trip"
            stopIds = listOf(child1Stop.id, child2Stop.id, child3Stop.id, "child4")
        }
        val routeType = MapStopRoute.matching(route)

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        objects.alert {
            activePeriod(
                Instant.parse("2024-03-18T04:30:00-04:00"),
                Instant.parse("2024-03-22T02:30:00-04:00"),
            )
            effect = Alert.Effect.Suspension
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                route = route.id,
                routeType = route.type,
                stop = child1Stop.id,
            )
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                route = route.id,
                routeType = route.type,
                stop = parent1Stop.id,
            )
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                route = route.id,
                routeType = route.type,
                stop = parent2Stop.id,
            )
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                route = route.id,
                routeType = route.type,
                stop = child2Stop.id,
            )
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                route = route.id,
                routeType = route.type,
                stop = parent3Stop.id,
            )
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                route = route.id,
                routeType = route.type,
                stop = child3Stop.id,
            )
        }

        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop =
                    mapOf(
                        Pair(child1Stop.id, listOf(routePattern.id)),
                        Pair(child2Stop.id, listOf(routePattern.id)),
                        Pair(child3Stop.id, listOf(routePattern.id)),
                    ),
            )
        val alertsByStop =
            GlobalMapData.getAlertsByStop(globalResponse, AlertsStreamDataResponse(objects), time)

        val terminalParent = alertsByStop?.get(parent1Stop.id)
        assertNotNull(terminalParent)
        assertEquals(StopAlertState.Suspension, terminalParent.stateByRoute[routeType])

        val regularParent = alertsByStop[parent2Stop.id]
        assertNotNull(regularParent)
        assertEquals(StopAlertState.Suspension, regularParent.stateByRoute[routeType])

        val boundaryParent = alertsByStop[parent3Stop.id]
        assertNotNull(boundaryParent)
        assertEquals(StopAlertState.Suspension, boundaryParent.stateByRoute[routeType])
    }

    @Test
    fun `getAlertAffectedStops resolves parents`() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        lateinit var childStop: Stop
        val parentStation = objects.stop { childStop = childStop() }
        val alert = objects.alert { informedEntity(listOf(), stop = childStop.id) }

        val affectedStops = GlobalResponse(objects).getAlertAffectedStops(alert, listOf(route))
        assertEquals(affectedStops, listOf(parentStation))
    }

    @Test
    fun `getAlertAffectedStops filters to selected route`() {
        val objects = ObjectCollectionBuilder()

        val route1 = objects.route()
        val route2 = objects.route()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val alert =
            objects.alert {
                informedEntity(listOf(), stop = stop1.id)
                informedEntity(listOf(), route = route1.id, stop = stop2.id)
                informedEntity(listOf(), route = route2.id, stop = stop3.id)
            }

        val affectedStops = GlobalResponse(objects).getAlertAffectedStops(alert, listOf(route1))
        assertEquals(affectedStops, listOf(stop1, stop2))
    }

    @Test
    fun `getAlertAffectedStops avoids duplicates`() {
        val objects = ObjectCollectionBuilder()

        val route1 = objects.route()
        val route2 = objects.route()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val alert =
            objects.alert {
                informedEntity(listOf(), route = route1.id, stop = stop1.id)
                informedEntity(listOf(), route = route1.id, stop = stop2.id)
                informedEntity(listOf(), route = route2.id, stop = stop1.id)
                informedEntity(listOf(), route = route2.id, stop = stop2.id)
            }

        val affectedStops =
            GlobalResponse(objects).getAlertAffectedStops(alert, listOf(route1, route2))
        assertEquals(affectedStops, listOf(stop1, stop2))
    }

    @Test
    fun `routesByLineId includes only non-shuttle routes`() {
        val objects = ObjectCollectionBuilder()

        val line = objects.line()

        val route = objects.route { lineId = line.id }

        val shuttleRoute =
            objects.route {
                id = "Shuttle-1"
                lineId = line.id
            }

        // not included b/c no line
        val otherRoute = objects.route()

        assertEquals(mapOf(line.id to listOf(route)), GlobalResponse(objects).routesByLineId)
    }

    @Test
    fun `getRoutesForPicker returns subway for Root path`() {
        val objects = TestData.clone()
        val global = GlobalResponse(objects)

        // Test data does not include mattapan trolley or blue line
        assertEquals(
            listOf(
                RouteCardData.LineOrRoute.Route(objects.getRoute("Red")),
                RouteCardData.LineOrRoute.Route(objects.getRoute("Orange")),
                RouteCardData.LineOrRoute.Line(
                    objects.getLine("line-Green"),
                    setOf(
                        objects.getRoute("Green-B"),
                        objects.getRoute("Green-C"),
                        objects.getRoute("Green-D"),
                        objects.getRoute("Green-E"),
                    ),
                ),
            ),
            global.getRoutesForPicker(RoutePickerPath.Root),
        )
    }

    @Test
    fun `getRoutesForPicker returns bus routes`() {
        val objects = TestData.clone()
        val global = GlobalResponse(objects)

        val busRoutes = global.getRoutesForPicker(RoutePickerPath.Bus)
        val silverEnd =
            busRoutes.takeLast(busRoutes.size - busRoutes.indexOfFirst { it.id in silverRoutes })

        assertTrue(busRoutes.all { it.type == RouteType.BUS })
        assertTrue(silverEnd.isNotEmpty() && silverEnd.size != busRoutes.size)
        assertTrue(silverEnd.all { it.id in silverRoutes })
    }

    @Test
    fun `getRoutesForPicker returns silver line routes`() {
        val objects = TestData.clone()
        val global = GlobalResponse(objects)

        val slRoutes = global.getRoutesForPicker(RoutePickerPath.Silver)
        assertTrue(slRoutes.isNotEmpty())
        assertTrue(slRoutes.all { it.id in silverRoutes })
    }

    @Test
    fun `getRoutesForPicker returns commuter routes`() {
        val objects = TestData.clone()
        val global = GlobalResponse(objects)

        val crRoutes = global.getRoutesForPicker(RoutePickerPath.CommuterRail)
        assertTrue(crRoutes.isNotEmpty())
        assertTrue(crRoutes.all { it.type == RouteType.COMMUTER_RAIL })
    }

    @Test
    fun `getRoutesForPicker returns ferry routes`() {
        val objects = TestData.clone()
        objects.route { type = RouteType.FERRY }
        val global = GlobalResponse(objects)

        val ferryRoutes = global.getRoutesForPicker(RoutePickerPath.Ferry)
        assertTrue(ferryRoutes.isNotEmpty())
        assertTrue(ferryRoutes.all { it.type == RouteType.FERRY })
    }

    @Test
    fun `getPatternsFor stop + route includes only patterns for that route`() {
        val objects = TestData.clone()

        val allPatternsForStop = global.getPatternsFor("place-haecl")
        assertTrue(allPatternsForStop.map { it.routeId }.any { it != "Orange" })
        val orangePatterns =
            global.getPatternsFor(
                "place-haecl",
                RouteCardData.LineOrRoute.Route(objects.getRoute("Orange")),
            )
        assertFalse(orangePatterns.map { it.routeId }.any { it != "Orange" })
    }

    @Test
    fun `getPatternsFor stop + line includes only patterns for that route`() {
        val objects = TestData.clone()

        val allPatternsForStop = global.getPatternsFor("place-haecl")
        assertTrue(allPatternsForStop.map { it.routeId }.any { it == "Orange" })
        val greenPatterns =
            global.getPatternsFor(
                "place-haecl",
                RouteCardData.LineOrRoute.Line(
                    objects.getLine("line-Green"),
                    setOf(
                        objects.getRoute("Green-B"),
                        objects.getRoute("Green-C"),
                        objects.getRoute("Green-D"),
                        objects.getRoute("Green-E"),
                    ),
                ),
            )
        assertTrue(greenPatterns.map { it.routeId }.all { it.startsWith("Green-") })
    }
}
