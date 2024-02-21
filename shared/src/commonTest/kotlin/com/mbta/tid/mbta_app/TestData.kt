package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.datetime.Instant

object TestData {
    fun prediction(
        id: String = uuid(),
        arrivalTime: Instant? = null,
        departureTime: Instant? = null,
        directionId: Int = 0,
        revenue: Boolean = true,
        scheduleRelationship: Prediction.ScheduleRelationship =
            Prediction.ScheduleRelationship.Scheduled,
        status: String? = null,
        stopSequence: Int? = null,
        stopId: String? = null,
        trip: Trip = trip(),
        vehicle: Vehicle? = null,
    ) =
        Prediction(
            id,
            arrivalTime,
            departureTime,
            directionId,
            revenue,
            scheduleRelationship,
            status,
            stopSequence,
            stopId,
            trip,
            vehicle
        )

    fun route(
        id: String = uuid(),
        color: String = "",
        directionNames: List<String> = emptyList(),
        directionDestinations: List<String> = emptyList(),
        longName: String = "",
        shortName: String = "",
        sortOrder: Int = 0,
        textColor: String = ""
    ) =
        Route(
            id,
            color,
            directionNames,
            directionDestinations,
            longName,
            shortName,
            sortOrder,
            textColor
        )

    fun routePattern(
        id: String = uuid(),
        directionId: Int = 0,
        name: String = "",
        sortOrder: Int = 0,
        representativeTrip: Trip? = null,
        routeId: String = ""
    ) = RoutePattern(id, directionId, name, sortOrder, representativeTrip, routeId)

    fun Route.pattern(
        id: String = uuid(),
        directionId: Int = 0,
        name: String = "",
        sortOrder: Int = 0,
        representativeTrip: Trip? = null
    ) = routePattern(id, directionId, name, sortOrder, representativeTrip, routeId = this.id)

    fun stop(
        id: String = uuid(),
        latitude: Double = 1.2,
        longitude: Double = 3.4,
        name: String = "",
        parentStation: Stop? = null
    ) = Stop(id, latitude, longitude, name, parentStation)

    fun trip(
        id: String = uuid(),
        headsign: String = "",
        routePatternId: String? = null,
        stops: List<Stop>? = null
    ) = Trip(id, headsign, routePatternId, stops)

    fun vehicle(
        id: String = uuid(),
        currentStatus: Vehicle.CurrentStatus,
        stopId: String? = null,
        tripId: String? = null
    ) = Vehicle(id, currentStatus, stopId, tripId)
}
