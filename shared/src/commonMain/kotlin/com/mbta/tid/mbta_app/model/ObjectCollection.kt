package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.uuid
import kotlinx.datetime.Instant

class ObjectCollection {
    val predictions = mutableMapOf<String, Prediction>()
    val routes = mutableMapOf<String, Route>()
    val routePatterns = mutableMapOf<String, RoutePattern>()
    val stops = mutableMapOf<String, Stop>()
    val trips = mutableMapOf<String, Trip>()
    val vehicles = mutableMapOf<String, Vehicle>()

    interface ObjectBuilder<Built : BackendObject> {
        fun built(): Built
    }

    class PredictionBuilder : ObjectBuilder<Prediction> {
        var id = uuid()
        var arrivalTime: Instant? = null
        var departureTime: Instant? = null
        var directionId = 0
        var revenue = true
        var scheduleRelationship = Prediction.ScheduleRelationship.Scheduled
        var status: String? = null
        var stopSequence: Int? = null
        var stopId = ""
        var tripId = ""
        var vehicleId: String? = null

        override fun built() =
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
                tripId,
                vehicleId
            )
    }

    fun prediction(block: PredictionBuilder.() -> Unit = {}) =
        build(predictions, PredictionBuilder(), block)

    class RouteBuilder : ObjectBuilder<Route> {
        var id = uuid()
        var type = RouteType.LIGHT_RAIL
        var color = ""
        var directionNames = listOf("", "")
        var directionDestinations = listOf("", "")
        var longName = ""
        var shortName = ""
        var sortOrder = 0
        var textColor = ""

        override fun built() =
            Route(
                id,
                type,
                color,
                directionNames,
                directionDestinations,
                longName,
                shortName,
                sortOrder,
                textColor
            )
    }

    fun route(block: RouteBuilder.() -> Unit = {}) = build(routes, RouteBuilder(), block)

    inner class RoutePatternBuilder : ObjectBuilder<RoutePattern> {
        var id: String = uuid()
        var directionId: Int = 0
        var name: String = ""
        var sortOrder: Int = 0
        var typicality: RoutePattern.Typicality? = RoutePattern.Typicality.Atypical
        var representativeTripId: String = ""
        var routeId: String = ""

        fun representativeTrip(block: TripBuilder.() -> Unit = {}) =
            this@ObjectCollection.trip {
                    routePatternId = this@RoutePatternBuilder.id
                    block()
                }
                .also { this.representativeTripId = it.id }

        override fun built() =
            RoutePattern(
                id,
                directionId,
                name,
                sortOrder,
                typicality,
                representativeTripId,
                routeId
            )
    }

    fun routePattern(route: Route, block: RoutePatternBuilder.() -> Unit = {}) =
        build(routePatterns, RoutePatternBuilder().apply { routeId = route.id }, block)

    class TripBuilder : ObjectBuilder<Trip> {
        var id = uuid()
        var headsign = ""
        var routePatternId = ""
        var stopIds: List<String>? = null

        override fun built() = Trip(id, headsign, routePatternId, stopIds)
    }

    fun trip(block: TripBuilder.() -> Unit = {}) = build(trips, TripBuilder(), block)

    class StopBuilder : ObjectBuilder<Stop> {
        var id = uuid()
        var latitude = 1.2
        var longitude = 3.4
        var name = ""
        var parentStationId: String? = null

        override fun built() = Stop(id, latitude, longitude, name, parentStationId)
    }

    fun stop(block: StopBuilder.() -> Unit = {}) = build(stops, StopBuilder(), block)

    class VehicleBuilder : ObjectBuilder<Vehicle> {
        var id: String = uuid()
        lateinit var currentStatus: Vehicle.CurrentStatus
        var stopId: String? = null
        var tripId = ""

        override fun built() = Vehicle(id, currentStatus, stopId, tripId)
    }

    fun vehicle(block: VehicleBuilder.() -> Unit = {}) = build(vehicles, VehicleBuilder(), block)

    private fun <Built : BackendObject, Builder : ObjectBuilder<Built>> build(
        source: MutableMap<String, Built>,
        builder: Builder,
        block: Builder.() -> Unit
    ): Built {
        builder.block()
        val result = builder.built()
        source[result.id] = result
        return result
    }

    object Single {
        fun prediction(block: PredictionBuilder.() -> Unit = {}) =
            ObjectCollection().prediction(block)

        fun route(block: RouteBuilder.() -> Unit = {}) = ObjectCollection().route(block)

        fun routePattern(route: Route, block: RoutePatternBuilder.() -> Unit = {}) =
            ObjectCollection().routePattern(route, block)

        fun trip(block: TripBuilder.() -> Unit = {}) = ObjectCollection().trip(block)

        fun stop(block: StopBuilder.() -> Unit = {}) = ObjectCollection().stop(block)

        fun vehicle(block: VehicleBuilder.() -> Unit = {}) = ObjectCollection().vehicle(block)
    }
}
