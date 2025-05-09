package com.mbta.tid.mbta_app.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object LoadingPlaceholders {
    fun nearbyRoute() =
        routeCardData(context = RouteCardData.Context.NearbyTransit, now = Clock.System.now())

    fun routeCardData(
        routeId: String? = null,
        trips: Int = 2,
        context: RouteCardData.Context,
        now: Instant,
    ): RouteCardData {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                routeId?.let { id = it }
                color = "000000"
                longName = "Loading"
                shortName = "00"
                textColor = "FFFFFF"
                directionNames = listOf("Loading", "Loading")
                directionDestinations = listOf("Loading", "Loading")
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
        val stop = objects.stop { name = "Loading" }

        fun newTrip(routePattern: RoutePattern, departsIn: Duration): UpcomingTrip {
            val trip = objects.trip(routePattern)
            val prediction =
                objects.prediction {
                    this.trip = trip
                    stopId = stop.id
                    departureTime = Clock.System.now() + departsIn
                }
            return UpcomingTrip(trip, prediction = prediction)
        }

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)

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

    fun stopDetailsRouteCards() =
        (1..5).map {
            routeCardData(
                context = RouteCardData.Context.StopDetailsUnfiltered,
                now = Clock.System.now(),
            )
        }

    data class TripDetailsInfo(
        val stops: TripDetailsStopList,
        val vehicle: Vehicle,
        val vehicleStop: Stop,
        val route: Route,
    )

    fun tripDetailsInfo(): TripDetailsInfo {
        val objects = ObjectCollectionBuilder()
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
                trip.id,
                (1..8).map { sequence ->
                    val stop = objects.stop { name = "Loading" }
                    val prediction =
                        objects.prediction {
                            this.trip = trip
                            this.stopId = stop.id
                            this.vehicleId = vehicle.id
                            departureTime = Clock.System.now() + sequence.minutes
                        }
                    TripDetailsStopList.Entry(
                        stop,
                        sequence,
                        disruption = null,
                        schedule = null,
                        prediction,
                        predictionStop = null,
                        vehicle,
                        listOf(otherRoute),
                    )
                },
            ),
            vehicle,
            vehicleStop,
            route,
        )
    }
}
