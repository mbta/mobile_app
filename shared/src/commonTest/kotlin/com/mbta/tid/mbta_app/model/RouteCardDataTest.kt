package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteCardDataTest {

    @Test
    fun `ListBuilder addStaticStopsData when there are no new  patterns for a stop then it is omitted`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Harvard"
                directionId = 0
            }
        }


        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id),
                    stop2.id to listOf(route1rp1.id),
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(
                                                route1rp1
                                            )
                                        )
                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData when second stop serves a new rp for route served by previous stop then include all rps at second stop`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Harvard"
                directionId = 0
            }
        }
        val route1rp2 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Nubian"
                directionId = 0
            }
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id),
                    stop2.id to listOf(route1rp1.id, route1rp2.id),
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(
                                                route1rp1
                                            )
                                        )
                                    )
                                ),
                                stop2.id to RouteCardData.RouteStopDataBuilder(
                                    stop2, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(
                                                route1rp1,
                                                route1rp2
                                            )
                                        )
                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData groups patterns by direction`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip { headsign = "Harvard" }
            directionId = 0
        }
        val route1rp2 = objects.routePattern(route1) {
            representativeTrip { headsign = "Harvard v2" }
            directionId = 0
        }
        val route1rp3 = objects.routePattern(route1) {
            representativeTrip { headsign = "Nubian" }
            directionId = 1
        }


        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(
                                                route1rp1,
                                                route1rp2
                                            )
                                        ),
                                        1 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(
                                                route1rp3
                                            )
                                        )

                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData when a stop is served by multiple routes it is included for each route`() {
        // TODO


    }


    @Test
    fun `ListBuilder addStaticStopsData groups by parent station`() {
        // TODO


    }


    @Test
    fun `ListBuilder addStaticStopsData preserves unscheduled physical platform`() {
        // TODO


    }


    @Test
    fun `ListBuilder addStaticStopsData Green Line shuttles are not grouped together`() {
        // TODO


    }

    @Test
    fun `ListBuilder addStaticStopsData Green Line routes are grouped together without Government Center direction`() {
        // TODO


    }

}
