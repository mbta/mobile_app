package com.mbta.tid.mbta_app.model

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

    fun patternsByStop(routeId: String? = null): PatternsByStop {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                routeId?.let { id = it }
                color = "000000"
                longName = "Loading"
                shortName = "00"
                textColor = "FFFFFF"
            }
        val pattern1 =
            objects.routePattern(route = route) { typicality = RoutePattern.Typicality.Typical }
        val pattern2 =
            objects.routePattern(route = route) { typicality = RoutePattern.Typicality.Typical }
        val stop = objects.stop { name = "Loading" }
        val trip1 = objects.trip(routePattern = pattern1)
        val prediction1 =
            objects.prediction {
                trip = trip1
                stopId = stop.id
                departureTime = Clock.System.now() + 5.minutes
            }
        val trip2 = objects.trip(routePattern = pattern1)
        val prediction2 =
            objects.prediction {
                trip = trip2
                stopId = stop.id
                departureTime = Clock.System.now() + 8.minutes
            }
        val trip3 = objects.trip(routePattern = pattern2)
        val prediction3 =
            objects.prediction {
                trip = trip3
                stopId = stop.id
                departureTime = Clock.System.now() + 5.minutes
            }
        val trip4 = objects.trip(routePattern = pattern2)
        val prediction4 =
            objects.prediction {
                trip = trip4
                stopId = stop.id
                departureTime = Clock.System.now() + 8.minutes
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
                        upcomingTrips =
                            listOf(
                                UpcomingTrip(trip = trip1, prediction = prediction1),
                                UpcomingTrip(trip = trip2, prediction = prediction2),
                            ),
                        alertsHere = emptyList(),
                        hasSchedulesToday = true,
                        allDataLoaded = false
                    ),
                    RealtimePatterns.ByHeadsign(
                        route = route,
                        headsign = "Loading 2",
                        line = null,
                        patterns = listOf(pattern2),
                        upcomingTrips =
                            listOf(
                                UpcomingTrip(trip = trip3, prediction = prediction3),
                                UpcomingTrip(trip = trip4, prediction = prediction4),
                            ),
                        alertsHere = emptyList(),
                        hasSchedulesToday = true,
                        allDataLoaded = false
                    )
                )
        )
    }

    fun stopDetailsDepartures(filter: StopDetailsFilter?) =
        if (filter != null) {
            StopDetailsDepartures(listOf(patternsByStop(filter.routeId)))
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
