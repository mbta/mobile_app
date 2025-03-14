package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
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
        val objects = ObjectCollectionBuilder()
        val arlington = objects.stop { id = "place-armnl" }
        val greenB =
            objects.route {
                id = "Green-B"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Government Center")
            }

        val savinHill = objects.stop { id = "place-shmnl" }
        val red =
            objects.route {
                id = "Red"
                directionNames = listOf("South", "North")
                directionDestinations = listOf("Ashmont/Braintree", "Alewife")
            }

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
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-bckhl" }
        objects.stop {
            id = "70199"
            parentStationId = "place-pktrm"
        }
        val route =
            objects.route {
                id = "Green-E"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Medford/Tufts")
            }
        val routePattern1 =
            objects.routePattern(route) {
                id = "rp1"
                representativeTripId = "trp1"
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
            }
        val routePattern2 =
            objects.routePattern(route) {
                id = "rp2"
                representativeTripId = "trp2"
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePattern3 =
            objects.routePattern(route) {
                id = "rp3"
                representativeTripId = "trp3"
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        objects.trip(routePattern1) {
            id = "trp1"
            stopIds = listOf("place-mdftf", "place-armnl", "place-bckhl", "place-hsmnl")
        }
        objects.trip(routePattern2) {
            id = "trp2"
            stopIds = listOf("place-mdftf", "place-prmnl", "place-bckhl", "place-hsmnl")
        }
        objects.trip(routePattern3) {
            id = "trp3"
            stopIds = listOf("place-hsmnl", "place-bckhl", "70199", "place-mdftf")
        }

        val patterns = listOf(routePattern1, routePattern2, routePattern3)
        val globalResponse =
            GlobalResponse(
                objects = objects,
                patternIdsByStop = mapOf(Pair(stop.id, patterns.map { pattern -> pattern.id }))
            )

        val directions = Direction.getDirections(globalResponse, stop, route, patterns)
        assertEquals("Heath Street", directions[0].destination)
        assertEquals("Gov Ctr & North", directions[1].destination)
    }

    @Test
    fun `atypical headsigns override route destination`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-bckhl" }
        objects.stop {
            id = "70199"
            parentStationId = "place-pktrm"
        }
        val route =
            objects.route {
                id = "Green-E"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Medford/Tufts")
            }
        val routePattern1 =
            objects.routePattern(route) {
                id = "rp1"
                representativeTripId = "trp1"
                directionId = 0
                typicality = RoutePattern.Typicality.Atypical
            }
        val routePattern2 =
            objects.routePattern(route) {
                id = "rp2"
                representativeTripId = "trp2"
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePattern3 =
            objects.routePattern(route) {
                id = "rp3"
                representativeTripId = "trp3"
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        objects.trip(routePattern1) {
            id = "trp1"
            headsign = "Other Headsign"
            stopIds = listOf("place-mdftf", "place-armnl", "place-bckhl", "place-hsmnl")
        }
        objects.trip(routePattern2) {
            id = "trp2"
            headsign = "Heath Street"
            stopIds = listOf("place-mdftf", "place-prmnl", "place-bckhl", "place-hsmnl")
        }
        objects.trip(routePattern3) {
            id = "trp3"
            headsign = "Medford/Tufts"
            stopIds = listOf("place-hsmnl", "place-bckhl", "70199", "place-mdftf")
        }

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
        assertEquals("Gov Ctr & North", directionRp3.destination)
    }

    @Test fun `getDirectionsForLine at different stops along the GL`() {
        val objects = ObjectCollectionBuilder()

        // B West
        val bc = objects.stop { id = "place-lake" }
        val southSt = objects.stop { id = "place-sougr" }

        // E West
        val heath = objects.stop { id = "place-hsmnl" }

        // Shared Trunk
        val hynes = objects.stop { id = "place-hymnl" }
        val arlington = objects.stop { id = "place-armnl" }
        val boylston = objects.stop {id = "place-boyls"}
        val gov = objects.stop { id = "place-gover" }


        // E East / North
        val haymarket = objects.stop { id = "place-haecl" }
        val lechmere = objects.stop { id = "place-lech" }
        val magoun = objects.stop { id = "place-mgngl" }

        val line = objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "Green-B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Government Center")
            }
        val routePatternB1 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B"
                    stopIds = listOf(gov.id, boylston.id, arlington.id, hynes.id, southSt.id,  bc.id)
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternB2 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B"
                    stopIds = listOf(bc.id, southSt.id, hynes.id, arlington.id,  boylston.id, gov.id)
                }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }

        val routeE =
            objects.route {
                id = "Green-E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath St", "Medford/Tufts")
            }
        val routePatternE1 =
            objects.routePattern(routeE) {
                id = "test-hs"
                representativeTrip { headsign = "Heath Street"
                    stopIds = listOf(magoun.id, lechmere.id, haymarket.id, gov.id, boylston.id, arlington.id, hynes.id, heath.id)

                }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternE2 =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Medford/Tufts"
                    stopIds = listOf(heath.id, hynes.id, arlington.id, boylston.id, gov.id, haymarket.id, lechmere.id, magoun.id)
                }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }

        val global = GlobalResponse(objects)

        // on Western branch
        assertEquals(listOf(Direction("West", "Boston College", 0), Direction("East", "Park St & North", 1)),
            Direction.getDirectionsForLine(global, southSt, listOf(routePatternB1, routePatternB2)))

        // on shard trunk
        assertEquals(listOf(Direction("West", "Kenmore & West", 0), Direction("East", "Park St & North", 1)),
            Direction.getDirectionsForLine(global, hynes, listOf(routePatternB1, routePatternB2, routePatternE1, routePatternE2)))

        // gov center extra special case
        assertEquals(listOf(Direction("West", "Copley & West", 0), Direction("East", "North Station & North", 1)),
            Direction.getDirectionsForLine(global, gov, listOf(routePatternB1, routePatternB2, routePatternE1, routePatternE2)))

        // eastern branch
        assertEquals(listOf(Direction("West", "Copley & West", 0), Direction("East", "Medford/Tufts", 1)),
            Direction.getDirectionsForLine(global, magoun, listOf(routePatternE1, routePatternE2)))
    }
}
