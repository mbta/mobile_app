package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.uuid
import io.github.dellisd.spatialk.geojson.Position
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
public class ObjectCollectionBuilder
private constructor(
    public val alerts: MutableMap<String, Alert>,
    public val facilities: MutableMap<String, Facility>,
    public val lines: MutableMap<String, Line>,
    public val predictions: MutableMap<String, Prediction>,
    public val routes: MutableMap<String, Route>,
    public val routePatterns: MutableMap<String, RoutePattern>,
    public val schedules: MutableMap<String, Schedule>,
    public val stops: MutableMap<String, Stop>,
    public val trips: MutableMap<String, Trip>,
    public val shapes: MutableMap<String, Shape>,
    public val vehicles: MutableMap<String, Vehicle>,
) {
    public constructor() :
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

    public fun clone(): ObjectCollectionBuilder =
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

    internal interface ObjectBuilder<Built : BackendObject> {
        fun built(): Built
    }

    public fun put(`object`: BackendObject) {
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

    public class AlertBuilder : ObjectBuilder<Alert> {
        public var id: String = uuid()
        public var activePeriod: MutableList<Alert.ActivePeriod> = mutableListOf()
        public var cause: Alert.Cause = Alert.Cause.UnknownCause
        public var description: String? = null
        public var durationCertainty: Alert.DurationCertainty = Alert.DurationCertainty.Unknown
        public var effect: Alert.Effect = Alert.Effect.UnknownEffect
        public var effectName: String? = null
        public var informedEntity: MutableList<Alert.InformedEntity> = mutableListOf()
        public var header: String? = null
        public var lifecycle: Alert.Lifecycle = Alert.Lifecycle.New
        public var severity: Int = 0
        public var updatedAt: EasternTimeInstant =
            EasternTimeInstant(Instant.fromEpochMilliseconds(0))
        public var facilities: Map<String, Facility>? = null

        public fun activePeriod(start: EasternTimeInstant, end: EasternTimeInstant?) {
            activePeriod.add(Alert.ActivePeriod(start, end))
        }

        @DefaultArgumentInterop.Enabled
        public fun informedEntity(
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

        override fun built(): Alert =
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

    public fun alert(block: AlertBuilder.() -> Unit): Alert = build(AlertBuilder(), block)

    public fun getAlert(id: String): Alert = alerts.getValue(id)

    public class FacilityBuilder : ObjectBuilder<Facility> {
        public var id: String = uuid()
        public var longName: String? = null
        public var shortName: String? = null
        public var type: Facility.Type = Facility.Type.Other

        override fun built(): Facility = Facility(id, longName, shortName, type)
    }

    @DefaultArgumentInterop.Enabled
    public fun facility(block: FacilityBuilder.() -> Unit = {}): Facility =
        build(FacilityBuilder(), block)

    public fun getFacility(id: String): Facility = facilities.getValue(id)

    public class LineBuilder : ObjectBuilder<Line> {
        public var id: String = uuid()
        public var color: String = "FFFFFF"
        public var longName: String = ""
        public var shortName: String = ""
        public var sortOrder: Int = 0
        public var textColor: String = "000000"

        override fun built(): Line = Line(id, color, longName, shortName, sortOrder, textColor)
    }

    @DefaultArgumentInterop.Enabled
    public fun line(block: LineBuilder.() -> Unit = {}): Line = build(LineBuilder(), block)

    public fun getLine(id: String): Line = lines.getValue(id)

    public inner class PredictionBuilder : ObjectBuilder<Prediction> {
        public var id: String = uuid()
        public var arrivalTime: EasternTimeInstant? = null
        public var departureTime: EasternTimeInstant? = null
        public var directionId: Int = 0
        public var revenue: Boolean = true
        public var scheduleRelationship: Prediction.ScheduleRelationship =
            Prediction.ScheduleRelationship.Scheduled
        public var status: String? = null
        public var stopSequence: Int = 0
        public var routeId: String = ""
        public var stopId: String = ""
        public var tripId: String = ""
        public var vehicleId: String? = null

        public var trip: Trip
            get() = checkNotNull(trips[tripId])
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
                directionId = trip.directionId
            }

        override fun built(): Prediction =
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
    public fun prediction(block: PredictionBuilder.() -> Unit = {}): Prediction =
        build(PredictionBuilder(), block)

    public fun prediction(
        schedule: Schedule,
        block: PredictionBuilder.() -> Unit = {},
    ): Prediction =
        build(
            PredictionBuilder().apply {
                routeId = schedule.routeId
                tripId = schedule.tripId
                stopId = schedule.stopId
                stopSequence = schedule.stopSequence
            },
            block,
        )

    public fun getPrediction(id: String): Prediction = predictions.getValue(id)

    public class RouteBuilder : ObjectBuilder<Route> {
        public var id: String = uuid()
        public var type: RouteType = RouteType.LIGHT_RAIL
        public var color: String = "FFFFFF"
        public var directionNames: List<String> = listOf("", "")
        public var directionDestinations: List<String> = listOf("", "")
        public var isListedRoute: Boolean = true
        public var longName: String = ""
        public var shortName: String = ""
        public var sortOrder: Int = 0
        public var textColor: String = "000000"
        public var lineId: String? = null
        public var routePatternIds: MutableList<String> = mutableListOf()

        override fun built(): Route =
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
    public fun route(block: RouteBuilder.() -> Unit = {}): Route = build(RouteBuilder(), block)

    public fun getRoute(id: String): Route = routes.getValue(id)

    public inner class RoutePatternBuilder : ObjectBuilder<RoutePattern> {
        public var id: String = uuid()
        public var directionId: Int = 0
        public var name: String = ""
        public var sortOrder: Int = 0
        public var typicality: RoutePattern.Typicality? = RoutePattern.Typicality.Atypical
        public var representativeTripId: String = ""
        public var routeId: String = ""

        public fun representativeTrip(block: TripBuilder.() -> Unit = {}): Trip =
            this@ObjectCollectionBuilder.trip {
                    routeId = this@RoutePatternBuilder.routeId
                    routePatternId = this@RoutePatternBuilder.id
                    directionId = this@RoutePatternBuilder.directionId
                    block()
                }
                .also { this.representativeTripId = it.id }

        override fun built(): RoutePattern =
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

    public fun routePattern(
        route: Route,
        block: RoutePatternBuilder.() -> Unit = {},
    ): RoutePattern = build(RoutePatternBuilder().apply { routeId = route.id }, block)

    public fun getRoutePattern(id: String): RoutePattern = routePatterns.getValue(id)

    public inner class ScheduleBuilder : ObjectBuilder<Schedule> {
        public var id: String = uuid()
        public var arrivalTime: EasternTimeInstant? = null
        public var departureTime: EasternTimeInstant? = null
        public var stopHeadsign: String? = null
        public var stopSequence: Int = 0
        public var routeId: String = ""
        public var stopId: String = ""
        public var tripId: String = ""

        public var trip: Trip
            get() = checkNotNull(trips[tripId])
            set(trip) {
                routePatterns[trip.routePatternId]?.routeId?.let { routeId = it }
                tripId = trip.id
            }

        override fun built(): Schedule =
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

    public fun schedule(block: ScheduleBuilder.() -> Unit = {}): Schedule =
        build(ScheduleBuilder(), block)

    public fun getSchedule(id: String): Schedule = schedules.getValue(id)

    public class TripBuilder : ObjectBuilder<Trip> {
        public var id: String = uuid()
        public var directionId: Int = 0
        public var headsign: String = ""
        public var routeId: String = ""
        public var routePatternId: String? = null
        public var shapeId: String? = null
        public var stopIds: List<String>? = null

        override fun built(): Trip =
            Trip(id, directionId, headsign, routeId, routePatternId, shapeId, stopIds)
    }

    public fun trip(block: TripBuilder.() -> Unit = {}): Trip = build(TripBuilder(), block)

    @DefaultArgumentInterop.Enabled
    public fun trip(routePattern: RoutePattern, block: TripBuilder.() -> Unit = {}): Trip =
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

    public fun getTrip(id: String): Trip = trips.getValue(id)

    public class ShapeBuilder : ObjectBuilder<Shape> {
        public var id: String = uuid()
        public var polyline: String = ""

        override fun built(): Shape = Shape(id, polyline)
    }

    public fun shape(block: ShapeBuilder.() -> Unit = {}): Shape = build(ShapeBuilder(), block)

    public fun getShape(id: String): Shape = shapes.getValue(id)

    public inner class StopBuilder : ObjectBuilder<Stop> {
        public var id: String = uuid()
        public var latitude: Double = 1.2
        public var longitude: Double = 3.4
        public var name: String = ""
        public var locationType: LocationType = LocationType.STOP
        public var description: String? = null
        public var platformCode: String? = null
        public var platformName: String? = null
        public var vehicleType: RouteType? = null
        public var childStopIds: List<String> = emptyList()
        public var connectingStopIds: List<String> = emptyList()
        public var parentStationId: String? = null
        public var wheelchairBoarding: WheelchairBoardingStatus? = null

        public var position: Position
            get() = Position(latitude = latitude, longitude = longitude)
            set(value) {
                latitude = value.latitude
                longitude = value.longitude
            }

        public fun childStop(block: StopBuilder.() -> Unit = {}): Stop =
            this@ObjectCollectionBuilder.stop {
                    parentStationId = this@StopBuilder.id
                    block()
                }
                .also { childStopIds += listOf(it.id) }

        override fun built(): Stop =
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

    public fun stop(block: StopBuilder.() -> Unit = {}): Stop = build(StopBuilder(), block)

    public fun getStop(id: String): Stop = stops.getValue(id)

    public class VehicleBuilder : ObjectBuilder<Vehicle> {
        public var id: String = uuid()
        public var bearing: Double = 0.0
        public lateinit var currentStatus: Vehicle.CurrentStatus
        public var currentStopSequence: Int? = null
        public var directionId: Int = 0
        public var latitude: Double = 1.2
        public var longitude: Double = 3.4
        public var updatedAt: EasternTimeInstant = EasternTimeInstant.now() - 10.seconds
        public var routeId: String? = null
        public var stopId: String? = null
        public var tripId: String = ""

        override fun built(): Vehicle =
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

    public fun vehicle(block: VehicleBuilder.() -> Unit = {}): Vehicle =
        build(VehicleBuilder(), block)

    public fun getVehicle(id: String): Vehicle = vehicles.getValue(id)

    @DefaultArgumentInterop.Enabled
    public fun upcomingTrip(
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

    public fun upcomingTrip(prediction: Prediction, predictionStop: Stop? = null): UpcomingTrip =
        upcomingTrip(null, prediction, predictionStop, null)

    private fun <Built : BackendObject, Builder : ObjectBuilder<Built>> build(
        builder: Builder,
        block: Builder.() -> Unit,
    ): Built {
        builder.block()
        val result = builder.built()
        return result.also(this::put)
    }

    public object Single {
        public fun alert(block: AlertBuilder.() -> Unit = {}): Alert =
            ObjectCollectionBuilder().alert(block)

        public fun facility(block: FacilityBuilder.() -> Unit = {}): Facility =
            ObjectCollectionBuilder().facility(block)

        public fun line(block: LineBuilder.() -> Unit = {}): Line =
            ObjectCollectionBuilder().line(block)

        public fun prediction(block: PredictionBuilder.() -> Unit = {}): Prediction =
            ObjectCollectionBuilder().prediction(block)

        public fun route(block: RouteBuilder.() -> Unit = {}): Route =
            ObjectCollectionBuilder().route(block)

        public fun routePattern(
            route: Route,
            block: RoutePatternBuilder.() -> Unit = {},
        ): RoutePattern = ObjectCollectionBuilder().routePattern(route, block)

        public fun trip(block: TripBuilder.() -> Unit = {}): Trip =
            ObjectCollectionBuilder().trip(block)

        public fun shape(block: ShapeBuilder.() -> Unit = {}): Shape =
            ObjectCollectionBuilder().shape(block)

        public fun schedule(block: ScheduleBuilder.() -> Unit = {}): Schedule =
            ObjectCollectionBuilder().schedule(block)

        public fun stop(block: StopBuilder.() -> Unit = {}): Stop =
            ObjectCollectionBuilder().stop(block)

        public fun vehicle(block: VehicleBuilder.() -> Unit = {}): Vehicle =
            ObjectCollectionBuilder().vehicle(block)
    }
}
