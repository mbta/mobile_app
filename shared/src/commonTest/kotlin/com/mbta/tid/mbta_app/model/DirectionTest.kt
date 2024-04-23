package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class DirectionTest {
    @Test
    fun `basic case gets correct values`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "1" }
        val route =
            objects.route {
                directionNames = listOf("Wrong Name", "Right Name")
                directionDestinations = listOf("Wrong Destination", "Right Destination")
            }

        val direction = Direction(1, stop, route, null)
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
                arlington,
                greenB,
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
                arlington,
                greenB,
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
                savinHill,
                red,
                listOf("place-alfcl", "place-jfk", "place-shmnl", "place-asmnl")
            )
        assertEquals("South", rlSouth.name)
        assertEquals("Ashmont", rlSouth.destination)

        val rlNorth =
            Direction(
                1,
                savinHill,
                red,
                listOf("place-asmnl", "place-shmnl", "place-jfk", "place-alfcl")
            )
        assertEquals("North", rlNorth.name)
        assertEquals("Alewife", rlNorth.destination)
    }

    @Test
    fun `both direction helper provides correct values`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-bckhl" }
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
            stopIds = listOf("place-hsmnl", "place-bckhl", "place-pktrm", "place-mdftf")
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
}
