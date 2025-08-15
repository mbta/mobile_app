package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GlobalMapDataTest {

    fun createData(): Pair<ObjectCollectionBuilder, GlobalResponse> {
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
                representativeTripId = "TS1"
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

        objects.trip(patternSilver) {
            id = patternSilver.representativeTripId
            stopIds = listOf(stopA1.id, stopC.id, stopD.id, stopE.id)
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
                            listOf(patternShuttle.id, patternCR.id, patternBus.id, patternRed.id),
                        ),
                        Pair(stopC.id, listOf(patternBus.id, patternSilver.id)),
                        Pair(stopD.id, listOf(patternSilver.id)),
                        Pair(stopE.id, listOf(patternSilver.id)),
                    ),
            )

        return Pair(objects, globalResponse)
    }

    @Test
    fun `mapStop data is created`() {

        val (objects, response) = createData()
        val routeSilver = objects.routes["742"]!!

        val mapData = GlobalMapData(globalData = response, mapOf())

        assertContains(
            mapData.mapStops["A"]!!.routeTypes,
            MapStopRoute.RED,
            "Route type should be associated with the stop it serves",
        )
        assertContains(
            mapData.mapStops["A"]!!.routeTypes,
            MapStopRoute.SILVER,
            "Silver line routes should be properly identified, and child routes included in parent",
        )
        assertContains(
            mapData.mapStops["A"]!!.routes[MapStopRoute.SILVER]!!,
            routeSilver,
            "Route type should be associated with the specific route that it was identified from",
        )
        assertEquals(
            listOf(MapStopRoute.RED, MapStopRoute.COMMUTER, MapStopRoute.BUS),
            mapData.mapStops["B"]!!.routeTypes,
            "Route types are ordered to match the route sort order",
        )

        assertFalse(
            mapData.mapStops["A1"]!!.routeTypes.contains(MapStopRoute.BLUE),
            "Atypical routes should not be included",
        )
        assertFalse(
            mapData.mapStops["A"]!!.routeTypes.contains(MapStopRoute.BUS),
            "Shuttle routes should not be included",
        )

        assertEquals(
            listOf(MapStopRoute.BUS),
            mapData.mapStops["C"]!!.routeTypes,
            "If a stop contains both SL and regular bus routes, only consider it a bus stop",
        )
        assertEquals(
            listOf(MapStopRoute.BUS),
            mapData.mapStops["D"]!!.routeTypes,
            "If a stop is a stop type and only has SL, only consider it a bus stop",
        )
        assertEquals(
            listOf(MapStopRoute.SILVER),
            mapData.mapStops["E"]!!.routeTypes,
            "If a stop is a station type and only has SL, consider it a SL station",
        )
    }

    @Test
    fun `mapStop data includes terminal stops`() {
        val (_, response) = createData()

        val mapData = GlobalMapData(globalData = response, mapOf())

        assertTrue(mapData.mapStops["A"]!!.isTerminal)
        assertFalse(mapData.mapStops["C"]!!.isTerminal)
        assertTrue(mapData.mapStops["E"]!!.isTerminal)
    }
}
