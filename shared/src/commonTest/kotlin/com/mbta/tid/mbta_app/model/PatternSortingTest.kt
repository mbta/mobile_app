package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class PatternSortingTest {
    var objects = ObjectCollectionBuilder()

    private fun singleRoute() = objects.routes.values.single()

    private fun singleLineOrRoute() =
        objects.lines.values.singleOrNull()?.let {
            RouteCardData.LineOrRoute.Line(it, objects.routes.values.toSet())
        } ?: objects.routes.values.single().let { RouteCardData.LineOrRoute.Route(it) }

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
                departureTime = EasternTimeInstant.now() + 4.minutes
            }
        )

    private fun leaf(
        lineOrRoute: RouteCardData.LineOrRoute = singleLineOrRoute(),
        stop: Stop = singleStop(),
        pattern: RoutePattern,
        trips: Int = 0,
        alertHere: Boolean = false,
        allDataLoaded: Boolean = true,
        hasSchedulesToday: Boolean = trips > 0,
    ) =
        RouteCardData.Leaf(
            lineOrRoute = lineOrRoute,
            stop = stop,
            directionId = pattern.directionId,
            routePatterns = listOf(pattern),
            stopIds = emptySet(),
            upcomingTrips = (1..trips).map { trip(pattern) },
            alertsHere =
                if (alertHere) listOf(objects.alert { effect = Alert.Effect.Suspension })
                else emptyList(),
            allDataLoaded = allDataLoaded,
            hasSchedulesToday = hasSchedulesToday,
            alertsDownstream = emptyList(),
            context = RouteCardData.Context.NearbyTransit,
        )

    private fun stopData(stop: Stop, vararg leaf: RouteCardData.Leaf) =
        stopData(stop, singleLineOrRoute(), *leaf)

    private fun stopData(
        stop: Stop,
        lineOrRoute: RouteCardData.LineOrRoute,
        vararg leaf: RouteCardData.Leaf,
    ) = RouteCardData.RouteStopData(lineOrRoute, stop, leaf.asList(), GlobalResponse(objects))

    private fun routeCard(route: Route, vararg stops: RouteCardData.RouteStopData) =
        routeCard(RouteCardData.LineOrRoute.Route(route), *stops)

    private fun routeCard(
        lineOrRoute: RouteCardData.LineOrRoute,
        vararg stops: RouteCardData.RouteStopData,
    ) = RouteCardData(lineOrRoute, stops.asList(), EasternTimeInstant.now())

    @Test
    fun compareLeavesAtStop() {
        objects.route()
        objects.stop()
        val pattern0 = pattern(directionId = 0, sortOrder = 0)
        val alert0 = leaf(pattern = pattern0, alertHere = true)
        val trip0 = leaf(pattern = pattern0, trips = 1)
        val ended0 = leaf(pattern = pattern0, hasSchedulesToday = true)
        val noService0 = leaf(pattern = pattern0)
        val pattern1 = pattern(directionId = 1, sortOrder = 0)
        val alert1 = leaf(pattern = pattern1, alertHere = true)
        val trip1 = leaf(pattern = pattern1, trips = 1)
        val ended1 = leaf(pattern = pattern1, hasSchedulesToday = true)
        val noService1 = leaf(pattern = pattern1)

        assertEquals(0, PatternSorting.compareLeavesAtStop().compare(alert0, trip0))
        assertEquals(0, PatternSorting.compareLeavesAtStop().compare(alert1, trip1))

        val expected = listOf(trip0, trip1, ended0, ended1, noService0, noService1)
        assertEquals(expected, expected.reversed().sortedWith(PatternSorting.compareLeavesAtStop()))
    }

    @Test
    fun compareStopsOnRoute() {
        objects.route()
        val position = Position(latitude = 0.0, longitude = 0.0)
        val nearStop =
            objects.stop {
                latitude = 0.1
                longitude = 0.1
            }
        val farStop =
            objects.stop {
                latitude = 2.0
                longitude = 2.0
            }

        val nearService0 =
            stopData(
                nearStop,
                leaf(stop = nearStop, pattern = pattern(directionId = 0, sortOrder = 0), trips = 1),
            )
        val nearService1 =
            stopData(
                nearStop,
                leaf(stop = nearStop, pattern = pattern(directionId = 1, sortOrder = 0), trips = 1),
            )
        val nearNoService =
            stopData(
                nearStop,
                leaf(stop = nearStop, pattern = pattern(directionId = 0, sortOrder = 0)),
            )
        val farService =
            stopData(
                farStop,
                leaf(stop = farStop, pattern = pattern(directionId = 0, sortOrder = 0), trips = 1),
            )

        assertEquals(
            0,
            PatternSorting.compareStopsOnRoute(null).compare(nearService0, nearService1),
        )
        assertEquals(
            0,
            PatternSorting.compareStopsOnRoute(position).compare(nearService0, nearService1),
        )
        assertEquals(0, PatternSorting.compareStopsOnRoute(null).compare(nearService0, farService))

        val expected = listOf(nearService0, farService, nearNoService)
        assertEquals(
            expected,
            expected.reversed().sortedWith(PatternSorting.compareStopsOnRoute(position)),
        )
    }

    enum class Service {
        Yes,
        Ended,
        No,
    }

    @Test
    fun compareRouteCards() {
        val position = Position(latitude = 0.0, longitude = 0.0)
        val nearStop =
            objects.stop {
                latitude = 0.1
                longitude = 0.1
            }
        val farStop =
            objects.stop {
                latitude = 2.0
                longitude = 2.0
            }
        val pinnedRoutes = mutableSetOf<String>()
        fun routeCard(
            pinned: Boolean,
            service: Service,
            subway: Boolean,
            near: Boolean,
            sortOrder: Int,
        ): RouteCardData {
            val route =
                objects.route {
                    type = if (subway) RouteType.HEAVY_RAIL else RouteType.BUS
                    this.sortOrder = sortOrder
                }
            val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
            if (pinned) pinnedRoutes.add(route.id)
            val stop = if (near) nearStop else farStop
            return routeCard(
                route,
                stopData(
                    stop,
                    RouteCardData.LineOrRoute.Route(route),
                    leaf(
                        lineOrRoute,
                        stop,
                        pattern(route, 0, 0),
                        trips = if (service == Service.Yes) 1 else 0,
                        hasSchedulesToday = service != Service.No,
                    ),
                ),
            )
        }
        val routeCard1 =
            routeCard(
                pinned = true,
                service = Service.Yes,
                subway = true,
                near = true,
                sortOrder = 1,
            )
        val routeCard2 =
            routeCard(
                pinned = true,
                service = Service.Yes,
                subway = true,
                near = true,
                sortOrder = 50,
            )
        val routeCard3 =
            routeCard(
                pinned = true,
                service = Service.Yes,
                subway = true,
                near = false,
                sortOrder = 1,
            )
        val routeCard4 =
            routeCard(
                pinned = true,
                service = Service.Yes,
                subway = false,
                near = true,
                sortOrder = 1,
            )
        val routeCard5 =
            routeCard(
                pinned = true,
                service = Service.Ended,
                subway = true,
                near = true,
                sortOrder = 1,
            )
        val routeCard6 =
            routeCard(
                pinned = true,
                service = Service.No,
                subway = true,
                near = true,
                sortOrder = 1,
            )
        val routeCard7 =
            routeCard(
                pinned = false,
                service = Service.Yes,
                subway = true,
                near = true,
                sortOrder = 1,
            )

        assertEquals(
            0,
            PatternSorting.compareRouteCards(emptySet(), null, RouteCardData.Context.NearbyTransit)
                .compare(routeCard1, routeCard3),
        )
        assertEquals(
            0,
            PatternSorting.compareRouteCards(
                    emptySet(),
                    position,
                    RouteCardData.Context.NearbyTransit,
                )
                .compare(routeCard1, routeCard7),
        )

        val expected =
            listOf(
                routeCard1,
                routeCard2,
                routeCard3,
                routeCard4,
                routeCard5,
                routeCard6,
                routeCard7,
            )
        assertEquals(
            expected,
            expected
                .reversed()
                .sortedWith(
                    PatternSorting.compareRouteCards(
                        pinnedRoutes,
                        position,
                        RouteCardData.Context.NearbyTransit,
                    )
                ),
        )
    }

    @Test
    fun compareRouteCardsOnFavoritesIgnoresRouteType() {
        val position = Position(latitude = 0.0, longitude = 0.0)
        val nearStop =
            objects.stop {
                latitude = 0.1
                longitude = 0.1
            }
        val farStop =
            objects.stop {
                latitude = 2.0
                longitude = 2.0
            }
        fun routeCard(subway: Boolean, near: Boolean, sortOrder: Int): RouteCardData {
            val route =
                objects.route {
                    type = if (subway) RouteType.HEAVY_RAIL else RouteType.BUS
                    this.sortOrder = sortOrder
                }
            val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
            val stop = if (near) nearStop else farStop
            return routeCard(
                route,
                stopData(
                    stop,
                    RouteCardData.LineOrRoute.Route(route),
                    leaf(
                        lineOrRoute,
                        stop,
                        pattern(route, 0, 0),
                        trips = 1,
                        hasSchedulesToday = true,
                    ),
                ),
            )
        }
        val routeCard1 = routeCard(subway = false, near = true, sortOrder = 10)
        val routeCard2 = routeCard(subway = true, near = false, sortOrder = 1)

        val expected = listOf(routeCard1, routeCard2)
        assertEquals(
            expected,
            expected
                .reversed()
                .sortedWith(
                    PatternSorting.compareRouteCards(
                        emptySet(),
                        position,
                        RouteCardData.Context.Favorites,
                    )
                ),
        )

        assertEquals(
            expected.reversed(),
            expected.sortedWith(
                PatternSorting.compareRouteCards(
                    emptySet(),
                    position,
                    RouteCardData.Context.NearbyTransit,
                )
            ),
        )
    }
}
