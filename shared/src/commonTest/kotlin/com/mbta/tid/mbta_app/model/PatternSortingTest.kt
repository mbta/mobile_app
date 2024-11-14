package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.NearbyStaticData.StaticPatterns
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class PatternSortingTest {
    var objects = ObjectCollectionBuilder()

    private fun singleLine() = objects.lines.values.single()

    private fun singleLineOrNull() = objects.lines.values.singleOrNull()

    private fun singleRoute() = objects.routes.values.single()

    private fun singleStop() = objects.stops.values.single()

    @BeforeTest
    fun setup() {
        objects = ObjectCollectionBuilder()
    }

    private fun pattern(directionId: Int, sortOrder: Int) =
        pattern(singleRoute(), directionId, sortOrder)

    private fun pattern(route: Route, directionId: Int, sortOrder: Int) =
        objects.routePattern(route) {
            this.directionId = directionId
            this.sortOrder = sortOrder
        }

    private fun trip(routePattern: RoutePattern) =
        objects.upcomingTrip(
            objects.schedule {
                trip = objects.trip(routePattern)
                departureTime = Clock.System.now() + 4.minutes
            }
        )

    private fun staticPatternsByDirection(direction: Direction, sortOrder: Int) =
        StaticPatterns.ByDirection(
            singleLine(),
            listOf(singleRoute()),
            direction,
            listOf(pattern(direction.id, sortOrder)),
            setOf(singleStop().id)
        )

    private fun staticPatternsByHeadsign(headsign: String, directionId: Int, sortOrder: Int) =
        StaticPatterns.ByHeadsign(
            singleRoute(),
            headsign,
            singleLineOrNull(),
            listOf(pattern(directionId, sortOrder)),
            setOf(singleStop().id)
        )

    @Test
    fun `test compareStaticPatterns`() {
        val line = objects.line()
        objects.route { lineId = line.id }
        objects.stop()
        val southbound = Direction("Southbound", "Ashmont/Braintree", 0)
        val northbound = Direction("Northbound", "Alewife", 1)
        val patterns1 = staticPatternsByDirection(southbound, 1)
        val patterns2 = staticPatternsByDirection(southbound, 2)
        val patterns3 = staticPatternsByHeadsign("Ashmont", 0, 1)
        val patterns4 = staticPatternsByDirection(northbound, 1)

        val expected =
            listOf(
                patterns1,
                patterns2,
                patterns3,
                patterns4,
            )

        val actual = expected.reversed().sortedWith(PatternSorting.compareStaticPatterns())
        assertEquals(expected, actual)
    }

    private fun realtimePatternsByDirection(
        direction: Direction,
        sortOrder: Int,
        trips: Int = 0,
        alerts: Int = 0,
        hasSchedulesToday: Boolean = trips > 0
    ): RealtimePatterns.ByDirection {
        val pattern = pattern(direction.id, sortOrder)
        val upcomingTrips = (1..trips).map { trip(pattern) }
        val alertsHere = (1..alerts).map { objects.alert { effect = Alert.Effect.Suspension } }
        return RealtimePatterns.ByDirection(
            singleLine(),
            listOf(singleRoute()),
            direction,
            listOf(pattern),
            upcomingTrips,
            alertsHere,
            alertsHere,
            hasSchedulesToday
        )
    }

    private fun realtimePatternsByHeadsign(
        headsign: String,
        directionId: Int,
        sortOrder: Int,
        trips: Int = 0,
        alerts: Int = 0,
        hasSchedulesToday: Boolean = trips > 0
    ) =
        realtimePatternsByHeadsign(
            singleRoute(),
            headsign,
            directionId,
            sortOrder,
            trips,
            alerts,
            hasSchedulesToday
        )

    private fun realtimePatternsByHeadsign(
        route: Route,
        headsign: String,
        directionId: Int,
        sortOrder: Int,
        trips: Int = 0,
        alerts: Int = 0,
        hasSchedulesToday: Boolean = trips > 0
    ): RealtimePatterns.ByHeadsign {
        val pattern = pattern(route, directionId, sortOrder)
        val upcomingTrips = (1..trips).map { trip(pattern) }
        val alertsHere = (1..alerts).map { objects.alert {} }
        return RealtimePatterns.ByHeadsign(
            route,
            headsign,
            singleLineOrNull(),
            listOf(pattern),
            upcomingTrips,
            alertsHere,
            alertsHere,
            hasSchedulesToday
        )
    }

    @Test
    fun `test compareRealtimePatterns`() {
        val line = objects.line()
        objects.route { lineId = line.id }
        objects.stop()
        val southbound = Direction("Southbound", "Ashmont/Braintree", 0)
        val northbound = Direction("Northbound", "Alewife", 1)
        // also checking if alerts and trips are treated as equal
        val patterns1 = realtimePatternsByDirection(southbound, 1, trips = 1)
        val patterns2 = realtimePatternsByDirection(southbound, 2, alerts = 1)
        val patterns3 = realtimePatternsByDirection(southbound, 3, trips = 1)
        val patterns4 = realtimePatternsByHeadsign("Ashmont", 0, 1, trips = 1)
        val patterns5 = realtimePatternsByDirection(northbound, 1, trips = 1)
        val patterns6 = realtimePatternsByDirection(southbound, 1, hasSchedulesToday = true)
        val patterns7 = realtimePatternsByDirection(southbound, 1)

        val expected =
            listOf(patterns1, patterns2, patterns3, patterns4, patterns5, patterns6, patterns7)
        val actual = expected.reversed().sortedWith(PatternSorting.compareRealtimePatterns())
        assertEquals(expected, actual)
    }

    @Test
    fun `test comparePatternsByStop`() {
        val route1 = objects.route { type = RouteType.HEAVY_RAIL }
        val route2 = objects.route { type = RouteType.BUS }
        val route3 = objects.route { type = RouteType.LIGHT_RAIL }
        val pinnedRoutes = setOf(route1.id, route2.id)
        val stop1 =
            objects.stop {
                latitude = 0.0
                longitude = 1.0
            }
        val stop2 =
            objects.stop {
                latitude = 0.0
                longitude = 2.0
            }
        // pinned, has service, subway, near, direction 0 (direction is from pattern fallback)
        val patternsByStop1 =
            PatternsByStop(
                route1,
                stop1,
                listOf(realtimePatternsByHeadsign(route1, "Ashmont", 0, 1, trips = 1))
            )
        // pinned, has service, subway, near, direction 1
        val patternsByStop2 =
            PatternsByStop(
                route1,
                stop1,
                listOf(realtimePatternsByHeadsign(route1, "Alewife", 1, 1, trips = 1))
            )
        // pinned, has service, subway, far, direction 0
        val patternsByStop3 =
            PatternsByStop(
                route1,
                stop2,
                listOf(realtimePatternsByHeadsign(route1, "Ashmont", 0, 1, trips = 1))
            )
        // pinned, has service, not subway, near, direction 0
        val patternsByStop4 =
            PatternsByStop(
                route2,
                stop1,
                listOf(realtimePatternsByHeadsign(route2, "", 0, 1, trips = 1))
            )
        // pinned, service ended, subway, near, direction 0
        val patternsByStop5 =
            PatternsByStop(
                route1,
                stop1,
                listOf(
                    realtimePatternsByHeadsign(route1, "Ashmont", 0, 1, hasSchedulesToday = true)
                )
            )
        // pinned, no service, subway, near, direction 0
        val patternsByStop6 =
            PatternsByStop(
                route1,
                stop1,
                listOf(realtimePatternsByHeadsign(route1, "Ashmont", 0, 1))
            )
        // unpinned, has service, subway, near, direction 0
        val patternsByStop7 =
            PatternsByStop(
                route3,
                stop1,
                listOf(realtimePatternsByHeadsign(route3, "Ashmont", 0, 1, trips = 1))
            )

        val expected =
            listOf(
                patternsByStop1,
                patternsByStop2,
                patternsByStop3,
                patternsByStop4,
                patternsByStop5,
                patternsByStop6,
                patternsByStop7
            )
        val actual =
            expected
                .reversed()
                .sortedWith(
                    PatternSorting.comparePatternsByStop(
                        pinnedRoutes,
                        Position(latitude = 0.0, longitude = 0.0)
                    )
                )
        assertEquals(expected, actual)
    }

    @Test
    fun `test compareTransitWithStops`() {
        val route1 =
            objects.route {
                type = RouteType.HEAVY_RAIL
                sortOrder = 1
            }
        val route2 =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 2
            }
        val route3 =
            objects.route {
                type = RouteType.BUS
                sortOrder = 1
            }

        val transitWithStops1 = NearbyStaticData.TransitWithStops.ByRoute(route1, listOf())
        val transitWithStops2 = NearbyStaticData.TransitWithStops.ByRoute(route2, listOf())
        val transitWithStops3 = NearbyStaticData.TransitWithStops.ByRoute(route3, listOf())

        val expected = listOf(transitWithStops1, transitWithStops2, transitWithStops3)
        val actual = expected.reversed().sortedWith(PatternSorting.compareTransitWithStops())
        assertEquals(expected, actual)
    }

    @Test
    fun `test compareStopsAssociated`() {
        val stop1 =
            objects.stop {
                latitude = 0.0
                longitude = 1.0
            }
        val stop2 =
            objects.stop {
                latitude = 0.0
                longitude = 2.0
            }
        val route1 =
            objects.route {
                type = RouteType.HEAVY_RAIL
                sortOrder = 1
            }
        val route2 =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 2
            }
        val route3 =
            objects.route {
                type = RouteType.BUS
                sortOrder = 1
            }
        val route4 =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 1
            }
        val pinnedRoutes = setOf(route1.id, route2.id, route3.id)
        // pinned, has service, subway, near, route sort order 1
        val stopsAssociated1 =
            StopsAssociated.WithRoute(
                route1,
                listOf(
                    PatternsByStop(
                        route1,
                        stop1,
                        listOf(realtimePatternsByHeadsign(route1, "", 0, 1, trips = 1))
                    )
                )
            )
        // pinned, has service, subway, near, route sort order 2
        val stopsAssociated2 =
            StopsAssociated.WithRoute(
                route2,
                listOf(
                    PatternsByStop(
                        route2,
                        stop1,
                        listOf(realtimePatternsByHeadsign(route2, "", 0, 1, trips = 1))
                    )
                )
            )
        // pinned, has service, subway, far, route sort order 1
        val stopsAssociated3 =
            StopsAssociated.WithRoute(
                route1,
                listOf(
                    PatternsByStop(
                        route1,
                        stop2,
                        listOf(realtimePatternsByHeadsign(route1, "", 0, 1, trips = 1))
                    )
                )
            )
        // pinned, has service, not subway, near, route sort order 1
        val stopsAssociated4 =
            StopsAssociated.WithRoute(
                route3,
                listOf(
                    PatternsByStop(
                        route3,
                        stop1,
                        listOf(realtimePatternsByHeadsign(route3, "", 0, 1, trips = 1))
                    )
                )
            )
        // pinned, service ended, subway, near, route sort order 1
        val stopsAssociated5 =
            StopsAssociated.WithRoute(
                route1,
                listOf(
                    PatternsByStop(
                        route1,
                        stop1,
                        listOf(
                            realtimePatternsByHeadsign(route1, "", 0, 1, hasSchedulesToday = true)
                        )
                    )
                )
            )
        // pinned, no service, subway, near, route sort order 1
        val stopsAssociated6 =
            StopsAssociated.WithRoute(
                route1,
                listOf(
                    PatternsByStop(
                        route1,
                        stop1,
                        listOf(realtimePatternsByHeadsign(route1, "", 0, 1))
                    )
                )
            )
        // not pinned, has service, subway, near, route sort order 1
        val stopsAssociated7 =
            StopsAssociated.WithRoute(
                route4,
                listOf(
                    PatternsByStop(
                        route4,
                        stop1,
                        listOf(realtimePatternsByHeadsign(route4, "", 0, 1, trips = 1))
                    )
                )
            )

        val expected =
            listOf(
                stopsAssociated1,
                stopsAssociated2,
                stopsAssociated3,
                stopsAssociated4,
                stopsAssociated5,
                stopsAssociated6,
                stopsAssociated7
            )
        val actual =
            expected
                .reversed()
                .sortedWith(
                    PatternSorting.compareStopsAssociated(
                        pinnedRoutes,
                        Position(latitude = 0.0, longitude = 0.0)
                    )
                )
        assertEquals(expected, actual)
    }
}
