package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.uuid
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
class ObjectCollectionBuilder
private constructor(
    val alerts: MutableMap<String, Alert>,
    val facilities: MutableMap<String, Facility>,
    val lines: MutableMap<String, Line>,
    val predictions: MutableMap<String, Prediction>,
    val routes: MutableMap<String, Route>,
    val routePatterns: MutableMap<String, RoutePattern>,
    val schedules: MutableMap<String, Schedule>,
    val stops: MutableMap<String, Stop>,
    val trips: MutableMap<String, Trip>,
    val shapes: MutableMap<String, Shape>,
    val vehicles: MutableMap<String, Vehicle>,
) {
    constructor() :
        this(
            alerts = mutableMapOf(),
            facilities = mutableMapOf(),
            lines = mutableMapOf(),
            predictions = mutableMapOf(),
            routes = mutableMapOf(),
            routePatterns = mutableMapOf(),
            schedules = mutableMapOf(),
            stops = mutableMapOf(),
            trips = mutableMapOf(),
            shapes = mutableMapOf(),
            vehicles = mutableMapOf(),
        )

    fun clone() =
        ObjectCollectionBuilder(
            alerts = alerts.toMutableMap(),
            facilities = facilities.toMutableMap(),
            lines = lines.toMutableMap(),
            predictions = predictions.toMutableMap(),
            routes = routes.toMutableMap(),
            routePatterns = routePatterns.toMutableMap(),
            schedules = schedules.toMutableMap(),
            stops = stops.toMutableMap(),
            trips = trips.toMutableMap(),
            shapes = shapes.toMutableMap(),
            vehicles = vehicles.toMutableMap(),
        )

    interface ObjectBuilder<Built : BackendObject> {
        fun built(): Built
    }

    fun put(`object`: BackendObject) {
        when (`object`) {
            is Alert -> alerts[`object`.id] = `object`
            is Facility -> facilities[`object`.id] = `object`
            is Line -> lines[`object`.id] = `object`
            is Prediction -> predictions[`object`.id] = `object`
            is Route -> routes[`object`.id] = `object`
            is RoutePattern -> routePatterns[`object`.id] = `object`
            is Schedule -> schedules[`object`.id] = `object`
            is Stop -> stops[`object`.id] = `object`
            is Trip -> trips[`object`.id] = `object`
            is Shape -> shapes[`object`.id] = `object`
            is Vehicle -> vehicles[`object`.id] = `object`
            else -> throw IllegalArgumentException("Can’t put unknown object ${`object`::class}")
        }
    }

    class AlertBuilder : ObjectBuilder<Alert> {
        var id = uuid()
        var activePeriod = mutableListOf<Alert.ActivePeriod>()
        var cause = Alert.Cause.UnknownCause
        var description: String? = null
        var durationCertainty = Alert.DurationCertainty.Unknown
        var effect = Alert.Effect.UnknownEffect
        var effectName: String? = null
        var informedEntity = mutableListOf<Alert.InformedEntity>()
        var header: String? = null
        var lifecycle = Alert.Lifecycle.New
        var severity = 0
        var updatedAt = Instant.fromEpochMilliseconds(0)
        var facilities: Map<String, Facility>? = null

        fun activePeriod(start: Instant, end: Instant?) {
            activePeriod.add(Alert.ActivePeriod(start, end))
        }

        @DefaultArgumentInterop.Enabled
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
                    trip,
                )
            )
        }

        override fun built() =
            Alert(
                id,
                activePeriod,
                cause,
                description,
                durationCertainty,
                effect,
                effectName,
                header,
                informedEntity,
                lifecycle,
                severity,
                updatedAt,
                facilities,
            )
    }

    fun alert(block: AlertBuilder.() -> Unit) = build(AlertBuilder(), block)

    fun getAlert(id: String) = alerts.getValue(id)

    class FacilityBuilder : ObjectBuilder<Facility> {
        var id = uuid()
        var longName: String? = null
        var shortName: String? = null
        var type: Facility.Type = Facility.Type.Other

        override fun built() = Facility(id, longName, shortName, type)
    }

    @DefaultArgumentInterop.Enabled
    fun facility(block: FacilityBuilder.() -> Unit = {}) = build(FacilityBuilder(), block)

    fun getFacility(id: String) = facilities.getValue(id)

    class LineBuilder : ObjectBuilder<Line> {
        var id = uuid()
        var color = "FFFFFF"
        var longName = ""
        var shortName = ""
        var sortOrder = 0
        var textColor = "000000"

        override fun built() = Line(id, color, longName, shortName, sortOrder, textColor)
    }

    @DefaultArgumentInterop.Enabled
    fun line(block: LineBuilder.() -> Unit = {}) = build(LineBuilder(), block)

    fun getLine(id: String) = lines.getValue(id)

    inner class PredictionBuilder : ObjectBuilder<Prediction> {
        var id = uuid()
        var arrivalTime: Instant? = null
        var departureTime: Instant? = null
        var directionId = 0
        var revenue = true
        var scheduleRelationship = Prediction.ScheduleRelationship.Scheduled
        var status: String? = null
        var stopSequence = 0
        var routeId = ""
        var stopId = ""
        var tripId = ""
        var vehicleId: String? = null

        var trip: Trip
            get() = checkNotNull(trips[tripId])
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
                directionId = trip.directionId
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

    @DefaultArgumentInterop.Enabled
    fun prediction(block: PredictionBuilder.() -> Unit = {}) = build(PredictionBuilder(), block)

    fun prediction(schedule: Schedule, block: PredictionBuilder.() -> Unit = {}) =
        build(
            PredictionBuilder().apply {
                routeId = schedule.routeId
                tripId = schedule.tripId
                stopId = schedule.stopId
                stopSequence = schedule.stopSequence
            },
            block,
        )

    fun getPrediction(id: String) = predictions.getValue(id)

    class RouteBuilder : ObjectBuilder<Route> {
        var id = uuid()
        var type = RouteType.LIGHT_RAIL
        var color = "FFFFFF"
        var directionNames = listOf("", "")
        var directionDestinations = listOf("", "")
        var isListedRoute = true
        var longName = ""
        var shortName = ""
        var sortOrder = 0
        var textColor = "000000"
        var lineId: String? = null
        var routePatternIds = mutableListOf<String>()

        override fun built() =
            Route(
                id,
                type,
                color,
                directionNames,
                directionDestinations,
                isListedRoute,
                longName,
                shortName,
                sortOrder,
                textColor,
                lineId,
                routePatternIds,
            )
    }

    @DefaultArgumentInterop.Enabled
    fun route(block: RouteBuilder.() -> Unit = {}) = build(RouteBuilder(), block)

    fun getRoute(id: String) = routes.getValue(id)

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
                    routeId = this@RoutePatternBuilder.routeId
                    routePatternId = this@RoutePatternBuilder.id
                    directionId = this@RoutePatternBuilder.directionId
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
        build(RoutePatternBuilder().apply { routeId = route.id }, block)

    fun getRoutePattern(id: String) = routePatterns.getValue(id)

    inner class ScheduleBuilder : ObjectBuilder<Schedule> {
        var id = uuid()
        var arrivalTime: Instant? = null
        var departureTime: Instant? = null
        var stopHeadsign: String? = null
        var stopSequence = 0
        var routeId = ""
        var stopId = ""
        var tripId = ""

        var trip: Trip
            get() = checkNotNull(trips[tripId])
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
            }

        override fun built() =
            Schedule(
                id,
                arrivalTime,
                departureTime,
                when (arrivalTime) {
                    null -> Schedule.StopEdgeType.Unavailable
                    else -> Schedule.StopEdgeType.Regular
                },
                when (departureTime) {
                    null -> Schedule.StopEdgeType.Unavailable
                    else -> Schedule.StopEdgeType.Regular
                },
                stopHeadsign,
                stopSequence,
                routeId,
                stopId,
                tripId,
            )
    }

    fun schedule(block: ScheduleBuilder.() -> Unit = {}) = build(ScheduleBuilder(), block)

    fun getSchedule(id: String) = schedules.getValue(id)

    class TripBuilder : ObjectBuilder<Trip> {
        var id = uuid()
        var directionId = 0
        var headsign = ""
        var routeId = ""
        var routePatternId: String? = null
        var shapeId: String? = null
        var stopIds: List<String>? = null

        override fun built() =
            Trip(id, directionId, headsign, routeId, routePatternId, shapeId, stopIds)
    }

    fun trip(block: TripBuilder.() -> Unit = {}) = build(TripBuilder(), block)

    @DefaultArgumentInterop.Enabled
    fun trip(routePattern: RoutePattern, block: TripBuilder.() -> Unit = {}) =
        build(
            TripBuilder().apply {
                directionId = routePattern.directionId
                routeId = routePattern.routeId
                routePatternId = routePattern.id
                val representativeTrip = trips[routePattern.representativeTripId]
                if (representativeTrip != null) {
                    headsign = representativeTrip.headsign
                    shapeId = representativeTrip.shapeId
                    stopIds = representativeTrip.stopIds
                }
            },
            block,
        )

    fun getTrip(id: String) = trips.getValue(id)

    class ShapeBuilder : ObjectBuilder<Shape> {
        var id = uuid()
        var polyline = ""

        override fun built() = Shape(id, polyline)
    }

    fun shape(block: ShapeBuilder.() -> Unit = {}) = build(ShapeBuilder(), block)

    fun getShape(id: String) = shapes.getValue(id)

    inner class StopBuilder : ObjectBuilder<Stop> {
        var id = uuid()
        var latitude = 1.2
        var longitude = 3.4
        var name = ""
        var locationType = LocationType.STOP
        var description: String? = null
        var platformCode: String? = null
        var platformName: String? = null
        var vehicleType: RouteType? = null
        var childStopIds: List<String> = emptyList()
        var connectingStopIds: List<String> = emptyList()
        var parentStationId: String? = null
        var wheelchairBoarding: WheelchairBoardingStatus? = null

        var position: Position
            get() = Position(latitude = latitude, longitude = longitude)
            set(value) {
                latitude = value.latitude
                longitude = value.longitude
            }

        fun childStop(block: StopBuilder.() -> Unit = {}) =
            this@ObjectCollectionBuilder.stop {
                    parentStationId = this@StopBuilder.id
                    block()
                }
                .also { childStopIds += listOf(it.id) }

        override fun built() =
            Stop(
                id,
                latitude,
                longitude,
                name,
                locationType,
                description,
                platformCode,
                platformName,
                vehicleType,
                childStopIds,
                connectingStopIds,
                parentStationId,
                wheelchairBoarding,
            )
    }

    fun stop(block: StopBuilder.() -> Unit = {}) = build(StopBuilder(), block)

    fun getStop(id: String) = stops.getValue(id)

    class VehicleBuilder : ObjectBuilder<Vehicle> {
        var id: String = uuid()
        var bearing = 0.0
        lateinit var currentStatus: Vehicle.CurrentStatus
        var currentStopSequence: Int? = null
        var directionId = 0
        var latitude = 1.2
        var longitude = 3.4
        var updatedAt = Clock.System.now() - 10.seconds
        var routeId: String? = null
        var stopId: String? = null
        var tripId = ""

        override fun built() =
            Vehicle(
                id,
                bearing,
                currentStatus,
                currentStopSequence,
                directionId,
                latitude,
                longitude,
                updatedAt,
                routeId,
                stopId,
                tripId,
            )
    }

    fun vehicle(block: VehicleBuilder.() -> Unit = {}) = build(VehicleBuilder(), block)

    fun getVehicle(id: String) = vehicles.getValue(id)

    @DefaultArgumentInterop.Enabled
    fun upcomingTrip(
        schedule: Schedule? = null,
        prediction: Prediction? = null,
        predictionStop: Stop? = null,
        vehicle: Vehicle? = null,
    ): UpcomingTrip {
        if (prediction != null && schedule != null) {
            check(schedule.tripId == prediction.tripId)
        }
        if (prediction != null && predictionStop != null) {
            check(prediction.stopId == predictionStop.id)
        }
        return UpcomingTrip(
            checkNotNull(trips[prediction?.tripId ?: schedule?.tripId]),
            schedule,
            prediction,
            predictionStop ?: stops[prediction?.stopId],
            vehicle,
        )
    }

    fun upcomingTrip(prediction: Prediction, predictionStop: Stop? = null): UpcomingTrip =
        upcomingTrip(null, prediction, predictionStop, null)

    private fun <Built : BackendObject, Builder : ObjectBuilder<Built>> build(
        builder: Builder,
        block: Builder.() -> Unit,
    ): Built {
        builder.block()
        val result = builder.built()
        return result.also(this::put)
    }

    object Single {
        fun alert(block: AlertBuilder.() -> Unit = {}) = ObjectCollectionBuilder().alert(block)

        fun facility(block: FacilityBuilder.() -> Unit = {}) =
            ObjectCollectionBuilder().facility(block)

        fun line(block: LineBuilder.() -> Unit = {}) = ObjectCollectionBuilder().line(block)

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
