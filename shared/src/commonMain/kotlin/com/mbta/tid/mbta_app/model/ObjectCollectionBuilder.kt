package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.uuid
import kotlinx.datetime.Instant

/**
 * Allows related objects to be built and tracked more conveniently. Provides default values where
 * reasonable.
 *
 * Included in `commonMain` so that it can be used from the Android and iOS apps in previews and
 * tests.
 *
 * Design philosophy:
 * - Objects which don't need to be referenced directly (e.g. representative trips in route
 *   patterns) are built in a nested builder
 * - Objects which need to be referenced directly but descend inherently from a parent (e.g. route
 *   patterns from routes) are built with a required parent argument
 */
class ObjectCollectionBuilder {
    val alerts = mutableMapOf<String, Alert>()
    val predictions = mutableMapOf<String, Prediction>()
    val routes = mutableMapOf<String, Route>()
    val routePatterns = mutableMapOf<String, RoutePattern>()
    val schedules = mutableMapOf<String, Schedule>()
    val stops = mutableMapOf<String, Stop>()
    val trips = mutableMapOf<String, Trip>()
    val shapes = mutableMapOf<String, Shape>()
    val vehicles = mutableMapOf<String, Vehicle>()

    interface ObjectBuilder<Built : BackendObject> {
        fun built(): Built
    }

    class AlertBuilder : ObjectBuilder<Alert> {
        var id = uuid()
        var activePeriod = mutableListOf<Alert.ActivePeriod>()
        var effect = Alert.Effect.UnknownEffect
        var effectName: String? = null
        var informedEntity = mutableListOf<Alert.InformedEntity>()
        var lifecycle = Alert.Lifecycle.New

        fun activePeriod(start: Instant, end: Instant?) {
            activePeriod.add(Alert.ActivePeriod(start, end))
        }

        fun informedEntity(
            activities: List<Alert.InformedEntity.Activity>,
            directionId: Int? = null,
            facility: String? = null,
            route: String? = null,
            routeType: RouteType? = null,
            stop: String? = null,
            trip: String? = null,
        ) {
            informedEntity.add(
                Alert.InformedEntity(
                    activities,
                    directionId,
                    facility,
                    route,
                    routeType,
                    stop,
                    trip
                )
            )
        }

        override fun built() =
            Alert(id, activePeriod, effect, effectName, informedEntity, lifecycle)
    }

    fun alert(block: AlertBuilder.() -> Unit) = build(alerts, AlertBuilder(), block)

    inner class PredictionBuilder : ObjectBuilder<Prediction> {
        var id = uuid()
        var arrivalTime: Instant? = null
        var departureTime: Instant? = null
        var directionId = 0
        var revenue = true
        var scheduleRelationship = Prediction.ScheduleRelationship.Scheduled
        var status: String? = null
        var stopSequence: Int? = null
        var routeId = ""
        var stopId = ""
        var tripId = ""
        var vehicleId: String? = null

        var trip: Trip
            get() = trips.getValue(tripId)
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
            }

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
                routeId,
                stopId,
                tripId,
                vehicleId,
            )
    }

    fun prediction(block: PredictionBuilder.() -> Unit = {}) =
        build(predictions, PredictionBuilder(), block)

    fun prediction(schedule: Schedule, block: PredictionBuilder.() -> Unit = {}) =
        build(
            predictions,
            PredictionBuilder().apply {
                routeId = schedule.routeId
                tripId = schedule.tripId
                stopId = schedule.stopId
                stopSequence = schedule.stopSequence
            },
            block
        )

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
        var routePatternIds = mutableListOf<String>()

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
                textColor,
                routePatternIds,
            )
    }

    @DefaultArgumentInterop.Enabled
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
            this@ObjectCollectionBuilder.trip {
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
                routeId,
            )
    }

    fun routePattern(route: Route, block: RoutePatternBuilder.() -> Unit = {}) =
        build(routePatterns, RoutePatternBuilder().apply { routeId = route.id }, block)

    inner class ScheduleBuilder : ObjectBuilder<Schedule> {
        var id = uuid()
        var arrivalTime: Instant? = null
        var departureTime: Instant? = null
        var dropOffType = Schedule.StopEdgeType.Regular
        var pickUpType = Schedule.StopEdgeType.Regular
        var stopSequence = 0
        var routeId = ""
        var stopId = ""
        var tripId = ""

        var trip: Trip
            get() = trips.getValue(tripId)
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
            }

        override fun built() =
            Schedule(
                id,
                arrivalTime,
                departureTime,
                dropOffType,
                pickUpType,
                stopSequence,
                routeId,
                stopId,
                tripId
            )
    }

    fun schedule(block: ScheduleBuilder.() -> Unit = {}) =
        build(schedules, ScheduleBuilder(), block)

    class TripBuilder : ObjectBuilder<Trip> {
        var id = uuid()
        var directionId = 0
        var headsign = ""
        var routePatternId: String? = null
        var shapeId: String? = null
        var stopIds: List<String>? = null

        override fun built() = Trip(id, directionId, headsign, routePatternId, shapeId, stopIds)
    }

    fun trip(block: TripBuilder.() -> Unit = {}) = build(trips, TripBuilder(), block)

    @DefaultArgumentInterop.Enabled
    fun trip(routePattern: RoutePattern, block: TripBuilder.() -> Unit = {}) =
        build(
            trips,
            TripBuilder().apply {
                routePatternId = routePattern.id
                val representativeTrip = trips[routePattern.representativeTripId]
                if (representativeTrip != null) {
                    headsign = representativeTrip.headsign
                    shapeId = representativeTrip.shapeId
                }
            },
            block
        )

    class ShapeBuilder : ObjectBuilder<Shape> {
        var id = uuid()
        var polyline = ""

        override fun built() = Shape(id, polyline)
    }

    fun shape(block: ShapeBuilder.() -> Unit = {}) = build(shapes, ShapeBuilder(), block)

    class StopBuilder : ObjectBuilder<Stop> {
        var id = uuid()
        var latitude = 1.2
        var longitude = 3.4
        var name = ""
        var locationType = LocationType.STOP
        var parentStationId: String? = null
        var childStopIds: List<String> = emptyList()

        override fun built() =
            Stop(id, latitude, longitude, name, locationType, parentStationId, childStopIds)
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
        fun alert(block: AlertBuilder.() -> Unit = {}) = ObjectCollectionBuilder().alert(block)

        fun prediction(block: PredictionBuilder.() -> Unit = {}) =
            ObjectCollectionBuilder().prediction(block)

        fun route(block: RouteBuilder.() -> Unit = {}) = ObjectCollectionBuilder().route(block)

        fun routePattern(route: Route, block: RoutePatternBuilder.() -> Unit = {}) =
            ObjectCollectionBuilder().routePattern(route, block)

        fun trip(block: TripBuilder.() -> Unit = {}) = ObjectCollectionBuilder().trip(block)

        fun shape(block: ShapeBuilder.() -> Unit = {}) = ObjectCollectionBuilder().shape(block)

        fun schedule(block: ScheduleBuilder.() -> Unit = {}) =
            ObjectCollectionBuilder().schedule(block)

        fun stop(block: StopBuilder.() -> Unit = {}) = ObjectCollectionBuilder().stop(block)

        fun vehicle(block: VehicleBuilder.() -> Unit = {}) =
            ObjectCollectionBuilder().vehicle(block)
    }
}
