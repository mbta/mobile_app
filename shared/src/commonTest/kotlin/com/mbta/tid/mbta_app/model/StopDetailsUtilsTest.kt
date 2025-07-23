package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking

class StopDetailsUtilsTest {
    @Test
    fun `autoStopFilter provides a default StopDetailsFilter given a single route and direction`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route()
            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }
            val time = Instant.parse("2024-03-19T14:16:17-04:00")

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id))),
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    emptySet(),
                    RouteCardData.Context.StopDetailsUnfiltered,
                )

            assertEquals(
                StopDetailsFilter(
                    routeId = route.id,
                    directionId = routePattern.directionId,
                    autoFilter = true,
                ),
                StopDetailsUtils.autoStopFilter(routeCardData),
            )
        }

    @Test
    fun `autoStopFilter provides a null stop filter value given multiple routes and directions`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route1 = objects.route()
            val routePattern1 =
                objects.routePattern(route1) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }
            val route2 = objects.route()
            val routePattern2 =
                objects.routePattern(route2) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "B" }
                }
            val time = Instant.parse("2024-03-19T14:16:17-04:00")

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)),
                    ),
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    emptySet(),
                    RouteCardData.Context.StopDetailsUnfiltered,
                )

            assertEquals(null, StopDetailsUtils.autoStopFilter(checkNotNull(routeCardData)))
        }

    @Test
    fun `autoTripFilter provides a trip filter with the first trip selected`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()

        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")
        val trip1 = objects.trip(routePattern1)
        val trip2 = objects.trip(routePattern2)
        val vehicle =
            objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
            vehicleId = vehicle.id
        }

        val global =
            GlobalResponse(objects, mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))

        val stopFilter = StopDetailsFilter(route2.id, routePattern2.directionId)

        val data =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsUnfiltered,
            )

        assertEquals(
            TripDetailsFilter(trip2.id, vehicle.id, 0, false),
            StopDetailsUtils.autoTripFilter(data, stopFilter, null, time, global),
        )
    }

    @Test
    fun `autoTripFilter provides a null trip filter when no stop filter exists`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()
        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val global =
            GlobalResponse(objects, mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsUnfiltered,
            )

        assertEquals(
            null,
            StopDetailsUtils.autoTripFilter(checkNotNull(routeCardData), null, null, time, global),
        )
    }

    @Test
    fun `autoTripFilter provides a null trip filter when no trips exists`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()
        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val global =
            GlobalResponse(objects, mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))
        val stopFilter = StopDetailsFilter(route1.id, routePattern1.directionId)

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            null,
            StopDetailsUtils.autoTripFilter(
                checkNotNull(routeCardData),
                stopFilter,
                null,
                time,
                global,
            ),
        )
    }

    @Test
    fun `autoTripFilter provides current trip filter when trip is still upcoming`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()

        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")
        val trip1 = objects.trip(routePattern1)
        val trip2 = objects.trip(routePattern2)
        val trip3 = objects.trip(routePattern2)
        val vehicle =
            objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip3
            departureTime = time.plus(9.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip3
            departureTime = time.plus(9.minutes)
            vehicleId = vehicle.id
        }

        val global =
            GlobalResponse(objects, mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))
        val stopFilter = StopDetailsFilter(route2.id, routePattern2.directionId)
        val currentTripFilter = TripDetailsFilter(trip3.id, vehicle.id, 0, false)

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            currentTripFilter,
            StopDetailsUtils.autoTripFilter(
                routeCardData,
                stopFilter,
                currentTripFilter,
                time,
                global,
            ),
        )
    }

    @Test
    fun `autoTripFilter sets vehicle when vehicle is newly assigned`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")
        val trip = objects.trip(routePattern)
        val vehicle =
            objects.vehicle {
                tripId = trip.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            this.trip = trip
            departureTime = time.plus(9.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            this.trip = trip
            departureTime = time.plus(9.minutes)
            vehicleId = vehicle.id
        }

        val global = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))
        val stopFilter = StopDetailsFilter(route.id, routePattern.directionId)
        val currentTripFilter = TripDetailsFilter(trip.id, vehicleId = null, 0, false)

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            TripDetailsFilter(trip.id, vehicle.id, 0, false),
            StopDetailsUtils.autoTripFilter(
                routeCardData,
                stopFilter,
                currentTripFilter,
                time,
                global,
            ),
        )
    }

    @Test
    fun `autoTripFilter provides next trip when current trip has passed the stop`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route()

        val routePattern1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val route2 = objects.route()
        val routePattern2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")
        val trip0 = objects.trip(routePattern2)
        val trip1 = objects.trip(routePattern1)
        val trip2 = objects.trip(routePattern2)

        val vehicle0 =
            objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicle1 =
            objects.vehicle {
                tripId = trip2.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip0
            departureTime = time.minus(3.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip0
            departureTime = time.minus(3.minutes)
            vehicleId = vehicle0.id
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
            vehicleId = vehicle1.id
        }

        val global =
            GlobalResponse(objects, mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))
        val stopFilter = StopDetailsFilter(route2.id, routePattern2.directionId)
        val currentTripFilter = TripDetailsFilter(trip0.id, vehicle0.id, 0, false)

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            TripDetailsFilter(trip2.id, vehicle1.id, 0, false),
            StopDetailsUtils.autoTripFilter(
                routeCardData,
                stopFilter,
                currentTripFilter,
                time,
                global,
            ),
        )
    }

    @Test
    fun `autoTripFilter provides current trip when current trip has passed the stop and is locked`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route1 = objects.route()

            val routePattern1 =
                objects.routePattern(route1) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }
            val route2 = objects.route()
            val routePattern2 =
                objects.routePattern(route2) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "B" }
                }

            val time = Instant.parse("2024-03-19T14:16:17-04:00")
            val trip0 = objects.trip(routePattern2)
            val trip1 = objects.trip(routePattern1)
            val trip2 = objects.trip(routePattern2)

            val vehicle0 =
                objects.vehicle {
                    tripId = trip0.id
                    currentStatus = Vehicle.CurrentStatus.InTransitTo
                }
            val vehicle1 =
                objects.vehicle {
                    tripId = trip2.id
                    currentStatus = Vehicle.CurrentStatus.InTransitTo
                }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip0
                departureTime = time.minus(3.minutes)
                vehicleId = vehicle0.id
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
                vehicleId = vehicle1.id
            }

            val global =
                GlobalResponse(
                    objects,
                    mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)),
                )
            val stopFilter = StopDetailsFilter(route2.id, routePattern2.directionId)
            val currentTripFilter = TripDetailsFilter(trip0.id, vehicle0.id, 0, true)

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    global,
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    emptySet(),
                    RouteCardData.Context.StopDetailsFiltered,
                )

            assertEquals(
                currentTripFilter,
                StopDetailsUtils.autoTripFilter(
                    routeCardData,
                    stopFilter,
                    currentTripFilter,
                    time,
                    global,
                ),
            )
        }

    @Test
    fun `autoTripFilter skips cancelled trips`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }

        val routePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
                representativeTrip { headsign = "A" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")
        val trip1 = objects.trip(routePattern)
        val trip2 = objects.trip(routePattern)
        val trip3 = objects.trip(routePattern)
        val trip4 = objects.trip(routePattern)
        val vehicle =
            objects.vehicle {
                tripId = trip3.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            departureTime = time.plus(2.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            departureTime = time.plus(5.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip3
            departureTime = time.plus(7.minutes)
        }
        objects.schedule {
            stopId = stop.id
            stopSequence = 0
            trip = trip4
            departureTime = time.plus(20.minutes)
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip1
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip2
            scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
        }
        objects.prediction {
            stopId = stop.id
            stopSequence = 0
            trip = trip3
            departureTime = time.plus(10.minutes)
            vehicleId = vehicle.id
        }

        val global = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))
        val stopFilter = StopDetailsFilter(route.id, routePattern.directionId)

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                time,
                emptySet(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            TripDetailsFilter(trip3.id, vehicle.id, 0, false),
            StopDetailsUtils.autoTripFilter(routeCardData, stopFilter, null, time, global),
        )
    }

    @Test
    fun `autoTripFilter selects the first cancelled trip if there are only cancelled trips`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route { type = RouteType.FERRY }

            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }

            val time = Instant.parse("2024-03-19T14:16:17-04:00")
            val trip1 = objects.trip(routePattern)
            val trip2 = objects.trip(routePattern)
            val trip3 = objects.trip(routePattern)
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                departureTime = time.plus(2.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                departureTime = time.plus(5.minutes)
            }
            objects.schedule {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                departureTime = time.plus(7.minutes)
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip1
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip2
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
            objects.prediction {
                stopId = stop.id
                stopSequence = 0
                trip = trip3
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }

            val global = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))
            val stopFilter = StopDetailsFilter(route.id, routePattern.directionId)

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    global,
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    emptySet(),
                    RouteCardData.Context.StopDetailsFiltered,
                )

            assertEquals(
                TripDetailsFilter(trip1.id, null, 0, false),
                StopDetailsUtils.autoTripFilter(routeCardData, stopFilter, null, time, global),
            )
        }

    @Test
    fun `filterVehiclesByUpcoming filters vehicles by relevant routes`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val stop = objects.stop()
        objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternB =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val tripB = objects.trip(routePatternB)

        val routeC =
            objects.route {
                id = "C"
                sortOrder = 2
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternC =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val tripC = objects.trip(routePatternC)

        val routeD =
            objects.route {
                id = "D"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Riverside", "Park St & North")
            }

        val routeE =
            objects.route {
                id = "E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Park St & North")
            }
        val routePatternE =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Heath Street" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                id = "test-hs"
            }
        val tripE = objects.trip(routePatternE)

        val time = Instant.parse("2024-03-18T10:41:13-04:00")

        val schedB =
            objects.schedule {
                trip = tripB
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val schedC =
            objects.schedule {
                trip = tripC
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }
        val schedE =
            objects.schedule {
                trip = tripE
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }

        objects.prediction(schedB) { departureTime = time + 1.5.minutes }
        objects.prediction(schedC) { departureTime = time + 2.3.minutes }
        objects.prediction(schedE) { departureTime = time + 2.3.minutes }

        val vehicleB =
            objects.vehicle {
                routeId = routeB.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleC =
            objects.vehicle {
                routeId = routeC.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleD =
            objects.vehicle {
                routeId = routeD.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleE =
            objects.vehicle {
                routeId = routeE.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
            }
        val vehicleResponse =
            VehiclesStreamDataResponse(
                mapOf(
                    vehicleB.id to vehicleB,
                    vehicleC.id to vehicleC,
                    vehicleD.id to vehicleD,
                    vehicleE.id to vehicleE,
                )
            )

        val global =
            GlobalResponse(
                objects,
                mapOf(stop.id to listOf(routePatternB.id, routePatternC.id, routePatternE.id)),
            )

        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(stop.id),
                global,
                sortByDistanceFrom = null,
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                now = time,
                setOf(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        assertEquals(
            mapOf(vehicleB.id to vehicleB, vehicleC.id to vehicleC, vehicleE.id to vehicleE),
            StopDetailsUtils.filterVehiclesByUpcoming(checkNotNull(routeCardData), vehicleResponse),
        )
    }
}
