package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StopTest {
    @Test
    fun `equalOrFamily handles equal stops without checking the map`() {
        assertTrue(Stop.equalOrFamily("a", "a", emptyMap()))
    }

    @Test
    fun `equalOrFamily handles parent-child`() {
        val objects = ObjectCollectionBuilder()

        val parent = objects.stop()
        val child = objects.stop { parentStationId = parent.id }

        assertTrue(Stop.equalOrFamily(parent.id, child.id, objects.stops))
        assertTrue(Stop.equalOrFamily(child.id, parent.id, objects.stops))
    }

    @Test
    fun `equalOrFamily handles siblings`() {
        val objects = ObjectCollectionBuilder()

        val parent = objects.stop()
        val child1 = objects.stop { parentStationId = parent.id }
        val child2 = objects.stop { parentStationId = parent.id }

        assertTrue(Stop.equalOrFamily(child1.id, child2.id, objects.stops))
        assertTrue(Stop.equalOrFamily(child2.id, child1.id, objects.stops))
    }

    @Test
    fun `equalOrFamily handles unrelated stops`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        assertFalse(Stop.equalOrFamily(stop1.id, stop2.id, objects.stops))
        assertFalse(Stop.equalOrFamily(stop2.id, stop1.id, objects.stops))
    }

    @Test
    fun `resolvedParentToAllStops builds map for station and and standalone stops`() {
        val objects = ObjectCollectionBuilder()

        val station =
            objects.stop {
                id = "station"
                locationType = LocationType.STATION
                childStopIds = listOf("child1")
            }
        val child1 =
            objects.stop {
                id = "child1"
                parentStationId = "station"
            }
        val standalone = objects.stop()

        assertEquals(
            mapOf(station to setOf(child1.id, station.id), standalone to setOf(standalone.id)),
            Stop.resolvedParentToAllStops(listOf(child1.id, standalone.id), GlobalResponse(objects)),
        )
    }

    @Test
    fun `isLastStopForAllPatterns true when last stop in every pattern appears in`() {
        val objects = ObjectCollectionBuilder()
        val targetParent =
            objects.stop {
                id = "targetParent"
                locationType = LocationType.STATION
                childStopIds = listOf("targetChild")
            }
        val targetChild =
            objects.stop {
                id = "targetChild"
                locationType = LocationType.STOP
                parentStationId = "targetParent"
            }

        val other1 = objects.stop {}
        val other2 = objects.stop {}
        val other3 = objects.stop {}

        val route = objects.route()

        val rp1 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    directionId = 0
                    stopIds = listOf(other1.id, other2.id, targetChild.id)
                }
            }

        val rp1Direction1 =
            objects.routePattern(route) {
                directionId = 1
                representativeTrip {
                    directionId = 1
                    stopIds = listOf(targetChild.id, other2.id, other1.id)
                }
            }

        val rp2 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    directionId = 0
                    stopIds = listOf(other1.id, other2.id, other3.id)
                }
            }

        val global = GlobalResponse(objects)

        assertTrue(
            targetParent.isLastStopForAllPatterns(0, listOf(rp1, rp1Direction1, rp2), global)
        )
    }

    @Test
    fun `isLastStopForAllPatterns false when mid-route in a pattern`() {
        val objects = ObjectCollectionBuilder()
        val targetParent =
            objects.stop {
                id = "targetParent"
                locationType = LocationType.STATION
                childStopIds = listOf("targetChild")
            }
        val targetChild =
            objects.stop {
                id = "targetChild"
                locationType = LocationType.STOP
                parentStationId = "targetParent"
            }

        val other1 = objects.stop {}
        val other2 = objects.stop {}
        val other3 = objects.stop {}

        val route = objects.route()

        val rp1 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    directionId = 0
                    stopIds = listOf(other1.id, targetChild.id, other2.id)
                }
            }

        val rp1Direction1 =
            objects.routePattern(route) {
                directionId = 1
                representativeTrip {
                    directionId = 1
                    stopIds = listOf(other2.id, targetChild.id, other1.id)
                }
            }

        val rp2 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    directionId = 0
                    stopIds = listOf(other1.id, other2.id, other3.id)
                }
            }

        val global = GlobalResponse(objects)

        assertFalse(
            targetParent.isLastStopForAllPatterns(0, listOf(rp1, rp1Direction1, rp2), global)
        )
    }
}
