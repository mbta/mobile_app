package com.mbta.tid.mbta_app.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

object LoadingPlaceholders {
    fun nearbyRoute(): StopsAssociated.WithRoute {
        val patternsByStop = patternsByStop()
        return StopsAssociated.WithRoute(
            route = patternsByStop.representativeRoute,
            patternsByStop = listOf(patternsByStop)
        )
    }

    fun patternsByStop(routeId: String? = null, trips: Int = 2): PatternsByStop {
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
            return UpcomingTrip(trip, prediction)
        }

        return PatternsByStop(
            route = route,
            stop = stop,
            patterns =
                listOf(
                    RealtimePatterns.ByHeadsign(
                        route = route,
                        headsign = "Loading 1",
                        line = null,
                        patterns = listOf(pattern1),
                        upcomingTrips = (1..trips).map { newTrip(pattern1, (it * 2).minutes) },
                        alertsHere = emptyList(),
                        hasSchedulesToday = true,
                        allDataLoaded = false
                    ),
                    RealtimePatterns.ByHeadsign(
                        route = route,
                        headsign = "Loading 2",
                        line = null,
                        patterns = listOf(pattern2),
                        upcomingTrips = (1..trips).map { newTrip(pattern2, (it * 2).minutes) },
                        alertsHere = emptyList(),
                        hasSchedulesToday = true,
                        allDataLoaded = false
                    )
                )
        )
    }

    fun stopDetailsDepartures(filter: StopDetailsFilter?) =
        if (filter != null) {
            StopDetailsDepartures(listOf(patternsByStop(filter.routeId, 10)))
        } else {
            StopDetailsDepartures((1..5).map { patternsByStop("loading-$it") })
        }

    fun tripDetailsStops(): TripDetailsStopList {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern = objects.routePattern(route)
        val trip = objects.trip(routePattern)
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = trip.id
            }
        val otherRoute =
            objects.route {
                color = "000000"
                longName = "Loading"
                shortName = "00"
                textColor = "FFFFFF"
                type = RouteType.COMMUTER_RAIL
            }
        return TripDetailsStopList(
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
                    alert = null,
                    schedule = null,
                    prediction,
                    vehicle,
                    listOf(otherRoute)
                )
            }
        )
    }
}
