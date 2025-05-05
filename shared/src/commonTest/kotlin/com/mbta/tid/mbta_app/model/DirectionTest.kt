package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class DirectionTest {
    @Test
    fun `basic case gets correct values`() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                directionNames = listOf("Wrong Name", "Right Name")
                directionDestinations = listOf("Wrong Destination", "Right Destination")
            }

        val direction = Direction(1, route)
        assertEquals("Right Name", direction.name)
        assertEquals("Right Destination", direction.destination)
    }

    @Test
    fun `special cases get correct values`() {
        val arlington = TestData.getStop("place-armnl")
        val greenB = TestData.getRoute("Green-B")

        val savinHill = TestData.getStop("place-shmnl")
        val red = TestData.getRoute("Red")

        val glWest =
            Direction(
                0,
                greenB,
                arlington,
                listOf(
                    "place-gover",
                    "place-pktrm",
                    "place-armnl",
                    "place-hymnl",
                    "place-kencl",
                    "place-lake"
                )
            )
        assertEquals("West", glWest.name)
        assertEquals("Copley & West", glWest.destination)

        val glEast =
            Direction(
                1,
                greenB,
                arlington,
                listOf(
                    "place-lake",
                    "place-kencl",
                    "place-hymnl",
                    "place-armnl",
                    "place-pktrm",
                    "place-gover"
                )
            )
        assertEquals("East", glEast.name)
        assertEquals("Gov Ctr & North", glEast.destination)

        val rlSouth =
            Direction(
                0,
                red,
                savinHill,
                listOf("place-alfcl", "place-jfk", "place-shmnl", "place-asmnl")
            )
        assertEquals("South", rlSouth.name)
        assertEquals("Ashmont", rlSouth.destination)

        val rlNorth =
            Direction(
                1,
                red,
                savinHill,
                listOf("place-asmnl", "place-shmnl", "place-jfk", "place-alfcl")
            )
        assertEquals("North", rlNorth.name)
        assertEquals("Alewife", rlNorth.destination)
    }

    @Test
    fun `both direction helper provides correct values`() {
        val objects = TestData.clone()
        val stop = objects.getStop("place-bckhl")
        val route = objects.getRoute("Green-E")
        val routePattern1 =
            objects.routePattern(route) {
                id = "rp1"
                representativeTrip {
                    stopIds = listOf("place-mdftf", "place-armnl", "place-bckhl", "place-hsmnl")
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
            }
        val routePattern2 = objects.getRoutePattern("Green-E-886-0")
        val routePattern3 = objects.getRoutePattern("Green-E-886-1")

        val patterns = listOf(routePattern1, routePattern2, routePattern3)
        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, patterns.map { pattern -> pattern.id }))
            )

        val directions = Direction.getDirections(globalResponse, stop, route, patterns)
        assertEquals("Heath Street", directions[0].destination)
        assertEquals("Park St & North", directions[1].destination)
    }

    @Test
    fun `atypical headsigns override route destination`() {
        val objects = TestData.clone()
        val stop = objects.getStop("place-bckhl")
        val route = objects.getRoute("Green-E")
        val routePattern1 =
            objects.routePattern(route) {
                id = "rp1"
                representativeTrip {
                    headsign = "Other Headsign"
                    stopIds = listOf("place-mdftf", "place-armnl", "place-bckhl", "place-hsmnl")
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
            }
        val routePattern2 = objects.getRoutePattern("Green-E-886-0")
        val routePattern3 = objects.getRoutePattern("Green-E-886-1")

        val patterns = listOf(routePattern1, routePattern2, routePattern3)
        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, patterns.map { pattern -> pattern.id }))
            )

        val directionRp1 =
            Direction.getDirectionForPattern(globalResponse, stop, route, routePattern1)
        val directionRp2 =
            Direction.getDirectionForPattern(globalResponse, stop, route, routePattern2)
        val directionRp3 =
            Direction.getDirectionForPattern(globalResponse, stop, route, routePattern3)
        assertEquals("Other Headsign", directionRp1.destination)
        assertEquals("Heath Street", directionRp2.destination)
        assertEquals("Park St & North", directionRp3.destination)
    }

    @Test
    fun `getDirectionsForLine at different stops along the GL`() {
        val objects = TestData.clone()

        // B West
        val southSt = objects.getStop("place-sougr")

        // Shared Trunk
        val kenmore = objects.getStop("place-kencl")
        val hynes = objects.getStop("place-hymnl")
        val gov = objects.getStop("place-gover")

        // E East / North
        val magoun = objects.getStop("place-mgngl")

        val routePatternB1 = objects.getRoutePattern("Green-B-812-0")
        val routePatternB2 = objects.getRoutePattern("Green-B-812-1")

        val routePatternC1 = objects.getRoutePattern("Green-C-832-0")
        val routePatternC2 = objects.getRoutePattern("Green-C-832-1")

        val routePatternE1 = objects.getRoutePattern("Green-E-886-0")
        val routePatternE2 = objects.getRoutePattern("Green-E-886-1")

        val global = GlobalResponse(objects)

        // on Western branch
        assertEquals(
            listOf(Direction("West", "Boston College", 0), Direction("East", "Park St & North", 1)),
            Direction.getDirectionsForLine(global, southSt, listOf(routePatternB1, routePatternB2))
        )

        // at Kenmore
        assertEquals(
            listOf(Direction("West", null, 0), Direction("East", "Park St & North", 1)),
            Direction.getDirectionsForLine(
                global,
                kenmore,
                listOf(routePatternB1, routePatternB2, routePatternC1, routePatternC2)
            )
        )

        // on shard trunk
        assertEquals(
            listOf(Direction("West", "Kenmore & West", 0), Direction("East", "Park St & North", 1)),
            Direction.getDirectionsForLine(
                global,
                hynes,
                listOf(routePatternB1, routePatternB2, routePatternC1, routePatternC2)
            )
        )

        // gov center extra special case
        assertEquals(
            listOf(
                Direction("West", "Copley & West", 0),
                Direction("East", "North Station & North", 1)
            ),
            Direction.getDirectionsForLine(
                global,
                gov,
                listOf(
                    routePatternB1,
                    routePatternB2,
                    routePatternC1,
                    routePatternC2,
                    routePatternE1,
                    routePatternE2
                )
            )
        )

        // eastern branch
        assertEquals(
            listOf(Direction("West", "Copley & West", 0), Direction("East", "Medford/Tufts", 1)),
            Direction.getDirectionsForLine(global, magoun, listOf(routePatternE1, routePatternE2))
        )
    }
}
