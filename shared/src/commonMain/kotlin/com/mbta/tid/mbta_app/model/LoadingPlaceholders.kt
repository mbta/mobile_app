package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.SearchViewModel
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public object LoadingPlaceholders {
    public fun nearbyRoute(): RouteCardData =
        routeCardData(context = RouteCardData.Context.NearbyTransit, now = EasternTimeInstant.now())

    public fun routeCardData(
        routeId: LineOrRoute.Id? = null,
        trips: Int = 2,
        context: RouteCardData.Context,
        now: EasternTimeInstant,
    ): RouteCardData {
        val objects = ObjectCollectionBuilder("LoadingPlaceholders.routeCardData")
        val line =
            if (routeId is Line.Id) {
                objects.line {
                    id = routeId.idText
                    color = "000000"
                    longName = "Loading"
                    shortName = "00"
                    textColor = "FFFFFF"
                }
            } else null
        val route =
            objects.route {
                if (routeId is Route.Id) {
                    id = routeId.idText
                }
                color = "000000"
                longName = "Loading"
                shortName = "00"
                textColor = "FFFFFF"
                directionNames = listOf("Loading", "Loading")
                directionDestinations = listOf("Loading", "Loading")
                if (routeId is Line.Id) {
                    lineId = routeId.idText
                }
            }
        val pattern1 =
            objects.routePattern(route = route) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
            }
        val pattern2 =
            objects.routePattern(route = route) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
            }
        val stop =
            objects.stop {
                name = "Loading"
                wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
            }

        fun newTrip(routePattern: RoutePattern, departsIn: Duration): UpcomingTrip {
            val trip = objects.trip(routePattern)
            val prediction =
                objects.prediction {
                    this.trip = trip
                    stopId = stop.id
                    departureTime = EasternTimeInstant.now() + departsIn
                }
            return UpcomingTrip(trip, prediction = prediction)
        }

        val lineOrRoute =
            if (line != null) LineOrRoute.Line(line, setOf(route)) else LineOrRoute.Route(route)

        val leaf1 =
            RouteCardData.Leaf(
                lineOrRoute = lineOrRoute,
                stop = stop,
                directionId = 0,
                routePatterns = listOf(pattern1),
                stopIds = setOf(stop.id),
                upcomingTrips = (1..trips).map { newTrip(pattern1, (it * 2).minutes) },
                alertsHere = emptyList(),
                allDataLoaded = false,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                context = context,
            )
        val leaf2 =
            RouteCardData.Leaf(
                lineOrRoute = lineOrRoute,
                stop = stop,
                directionId = 1,
                routePatterns = listOf(pattern2),
                stopIds = setOf(stop.id),
                upcomingTrips = (1..trips).map { newTrip(pattern2, (it * 2).minutes) },
                alertsHere = emptyList(),
                allDataLoaded = false,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                context = context,
            )

        val stopData =
            RouteCardData.RouteStopData(
                lineOrRoute,
                stop,
                listOf(Direction("Loading", null, 0), Direction("Loading", null, 1)),
                listOf(leaf1, leaf2),
            )

        val routeData = RouteCardData(lineOrRoute, stopData = listOf(stopData), now)

        return routeData
    }

    public fun routeDetailsStops(lineOrRoute: LineOrRoute, directionId: Int): RouteDetailsStopList {
        val objects = ObjectCollectionBuilder("LoadingPlaceholders.routeDetailsStops")
        val fixedRand = Random("${lineOrRoute.id}-$directionId".hashCode())
        fun randString(from: Int, until: Int) =
            (1..fixedRand.nextInt(from, until)).joinToString("") { " " }

        return RouteDetailsStopList(
            directionId = directionId,
            segments =
                listOf(
                    RouteDetailsStopList.Segment(
                        (1..20).map { number ->
                            val stopName = randString(15, 30)
                            val transferRoutes =
                                (0..fixedRand.nextInt(0, 7)).map {
                                    objects.route { shortName = randString(2, 5) }
                                }
                            RouteDetailsStopList.Entry(
                                objects.stop { name = stopName },
                                RouteBranchSegment.Lane.Center,
                                RouteBranchSegment.StickConnection.forward(
                                    stopBefore = "".takeUnless { number == 1 },
                                    stop = "",
                                    stopAfter = "".takeUnless { number == 20 },
                                    lane = RouteBranchSegment.Lane.Center,
                                ),
                                transferRoutes,
                            )
                        },
                        isTypical = true,
                    )
                ),
        )
    }

    public fun stopDetailsRouteCards(): List<RouteCardData> =
        (1..5).map {
            routeCardData(
                context = RouteCardData.Context.StopDetailsUnfiltered,
                now = EasternTimeInstant.now(),
            )
        }

    public fun stopResults(): List<SearchViewModel.StopResult> {
        val otherRoutePill =
            RoutePillSpec(
                textColor = "8A9199",
                routeColor = "8A9199",
                content = RoutePillSpec.Content.Text("Loading"),
                height = RoutePillSpec.Height.Small,
                width = RoutePillSpec.Width.Flex,
                shape = RoutePillSpec.Shape.Capsule,
                contentDescription = null,
            )
        return (1..10).map { index ->
            SearchViewModel.StopResult(
                "$index",
                isStation = index % 3 == 0,
                name = index.toString().repeat(index) + " Stop",
                routePills = listOf(otherRoutePill),
            )
        }
    }

    public data class TripDetailsInfo
    internal constructor(
        val stops: TripDetailsStopList,
        val trip: Trip,
        val vehicle: Vehicle,
        val vehicleStop: Stop,
        val route: Route,
    )

    public fun tripDetailsInfo(): TripDetailsInfo {
        val objects = ObjectCollectionBuilder("LoadingPlaceholders.tripDetailsInfo")
        val route =
            objects.route {
                color = "8A9199"
                longName = "Loading"
                shortName = "00"
                textColor = "8A9199"
            }
        val routePattern = objects.routePattern(route)
        val trip = objects.trip(routePattern)
        val vehicleStop = objects.stop { name = "Loading" }
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = vehicleStop.id
                tripId = trip.id
            }
        val otherRoute =
            objects.route {
                color = "8A9199"
                longName = "Loading"
                shortName = "00"
                textColor = "8A9199"
                type = RouteType.COMMUTER_RAIL
            }
        return TripDetailsInfo(
            TripDetailsStopList(
                trip,
                (1..8).map { sequence ->
                    val stop = objects.stop { name = "Loading" }
                    val prediction =
                        objects.prediction {
                            this.trip = trip
                            this.stopId = stop.id
                            this.vehicleId = vehicle.id
                            departureTime = EasternTimeInstant.now() + sequence.minutes
                        }
                    TripDetailsStopList.Entry(
                        stop,
                        sequence,
                        disruption = null,
                        schedule = null,
                        prediction,
                        vehicle = vehicle,
                        routes = listOf(otherRoute),
                    )
                },
            ),
            trip,
            vehicle,
            vehicleStop,
            route,
        )
    }
}
