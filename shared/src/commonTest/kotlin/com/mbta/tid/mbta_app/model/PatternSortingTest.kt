package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.maplibre.spatialk.geojson.Position

class PatternSortingTest {
    var objects = ObjectCollectionBuilder()

    private fun singleRoute() = objects.routes.values.single()

    private fun singleLineOrRoute() =
        objects.lines.values.singleOrNull()?.let {
            LineOrRoute.Line(it, objects.routes.values.toSet())
        } ?: objects.routes.values.single().let { LineOrRoute.Route(it) }

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
        lineOrRoute: LineOrRoute = singleLineOrRoute(),
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
                if (alertHere)
                    listOf(
                        objects.alert {
                            effect = Alert.Effect.Suspension
                            activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), null)
                        }
                    )
                else emptyList(),
            allDataLoaded = allDataLoaded,
            hasSchedulesToday = hasSchedulesToday,
            subwayServiceStartTime = null,
            alertsDownstream = emptyList(),
            context = RouteCardData.Context.NearbyTransit,
        )

    private fun stopData(stop: Stop, vararg leaf: RouteCardData.Leaf) =
        stopData(stop, singleLineOrRoute(), *leaf)

    private fun stopData(stop: Stop, lineOrRoute: LineOrRoute, vararg leaf: RouteCardData.Leaf) =
        RouteCardData.RouteStopData(lineOrRoute, stop, leaf.asList(), GlobalResponse(objects))

    private fun routeCard(route: Route, vararg stops: RouteCardData.RouteStopData) =
        routeCard(LineOrRoute.Route(route), *stops)

    private fun routeCard(lineOrRoute: LineOrRoute, vararg stops: RouteCardData.RouteStopData) =
        RouteCardData(lineOrRoute, stops.asList(), EasternTimeInstant.now())

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
        val veryNearStop =
            objects.stop {
                latitude = 0.0001
                longitude = 0.0001
            }
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

        val veryNearNoService =
            stopData(
                veryNearStop,
                leaf(stop = veryNearStop, pattern = pattern(directionId = 0, sortOrder = 0)),
            )
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

        val expected = listOf(veryNearNoService, nearService0, farService, nearNoService)
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
        val veryNearStop =
            objects.stop {
                latitude = 0.0001
                longitude = 0.0001
            }
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

        fun routeCard(
            service: Service,
            subway: Boolean,
            stop: Stop,
            sortOrder: Int,
        ): RouteCardData {
            val route =
                objects.route {
                    type = if (subway) RouteType.HEAVY_RAIL else RouteType.BUS
                    this.sortOrder = sortOrder
                }
            val lineOrRoute = LineOrRoute.Route(route)
            return routeCard(
                route,
                stopData(
                    stop,
                    LineOrRoute.Route(route),
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
        val routeCard0 =
            routeCard(service = Service.No, subway = false, stop = veryNearStop, sortOrder = 100)
        val routeCard1 =
            routeCard(service = Service.Yes, subway = true, stop = nearStop, sortOrder = 1)
        val routeCard2 =
            routeCard(service = Service.Yes, subway = true, stop = nearStop, sortOrder = 50)
        val routeCard3 =
            routeCard(service = Service.Yes, subway = true, stop = farStop, sortOrder = 1)
        val routeCard4 =
            routeCard(service = Service.Yes, subway = false, stop = nearStop, sortOrder = 1)
        val routeCard5 =
            routeCard(service = Service.Ended, subway = true, stop = nearStop, sortOrder = 1)
        val routeCard6 =
            routeCard(service = Service.No, subway = true, stop = nearStop, sortOrder = 1)

        assertEquals(
            0,
            PatternSorting.compareRouteCards(null, RouteCardData.Context.NearbyTransit)
                .compare(routeCard1, routeCard3),
        )

        val expected =
            listOf(
                routeCard0,
                routeCard1,
                routeCard2,
                routeCard3,
                routeCard4,
                routeCard5,
                routeCard6,
            )
        assertEquals(
            expected,
            expected
                .reversed()
                .sortedWith(
                    PatternSorting.compareRouteCards(position, RouteCardData.Context.NearbyTransit)
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
            val lineOrRoute = LineOrRoute.Route(route)
            val stop = if (near) nearStop else farStop
            return routeCard(
                route,
                stopData(
                    stop,
                    LineOrRoute.Route(route),
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
                    PatternSorting.compareRouteCards(position, RouteCardData.Context.Favorites)
                ),
        )

        assertEquals(
            expected.reversed(),
            expected.sortedWith(
                PatternSorting.compareRouteCards(position, RouteCardData.Context.NearbyTransit)
            ),
        )
    }
}
