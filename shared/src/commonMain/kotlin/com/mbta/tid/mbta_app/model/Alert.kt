package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.ServiceDateRounding
import com.mbta.tid.mbta_app.utils.serviceDate
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    override val id: String,
    @SerialName("active_period") val activePeriod: List<ActivePeriod>,
    val cause: Cause = Cause.UnknownCause,
    val description: String?,
    @SerialName("duration_certainty")
    val durationCertainty: DurationCertainty = DurationCertainty.Unknown,
    val effect: Effect = Effect.UnknownEffect,
    @SerialName("effect_name") val effectName: String?,
    val header: String?,
    @SerialName("informed_entity") val informedEntity: List<InformedEntity>,
    val lifecycle: Lifecycle,
    val severity: Int,
    @SerialName("updated_at") val updatedAt: Instant,
    // This field is not parsed from the Alert object from the backend, it is injected from
    // global data in the AlertsUsecase if any informed entities apply to a facility.
    val facilities: Map<String, Facility>? = null,
) : BackendObject {
    init {
        // This is done on init to avoid having to pass it in for any call to format an ActivePeriod
        activePeriod.forEach { period -> period.durationCertainty = durationCertainty }
    }

    val alertState: StopAlertState =
        when (this.effect) {
            Effect.ElevatorClosure -> StopAlertState.Elevator
            Effect.Shuttle -> StopAlertState.Shuttle
            in setOf(
                Effect.Suspension,
                Effect.StationClosure,
                Effect.StopClosure,
                Effect.DockClosure,
            ) -> StopAlertState.Suspension
            else -> StopAlertState.Issue
        }

    val hasStopsSpecified = informedEntity.all { it.stop != null }

    val significance =
        when (effect) {
            // suspensions or shuttles can reasonably apply to an entire route
            in setOf(Effect.Shuttle, Effect.Suspension) -> AlertSignificance.Major
            // detours and closures are only major if they specify stops
            in setOf(
                Effect.StationClosure,
                Effect.StopClosure,
                Effect.DockClosure,
                Effect.Detour,
                Effect.SnowRoute,
            ) -> if (hasStopsSpecified) AlertSignificance.Major else AlertSignificance.Secondary
            // service changes are always secondary
            Effect.ServiceChange -> AlertSignificance.Secondary
            Effect.ElevatorClosure -> AlertSignificance.Accessibility
            Effect.TrackChange -> AlertSignificance.Minor
            Effect.Delay ->
                if (
                    (severity >= 3 && informedEntity.any { it.routeType !== RouteType.BUS }) ||
                        cause == Cause.SingleTracking
                ) {
                    AlertSignificance.Minor
                } else {
                    AlertSignificance.None
                }
            else -> AlertSignificance.None
        }

    val hasNoThroughService = effect in setOf(Effect.Shuttle, Effect.Suspension)

    suspend fun summary(
        stopId: String,
        directionId: Int,
        patterns: List<RoutePattern>,
        atTime: Instant,
        global: GlobalResponse,
    ) = AlertSummary.summarizing(this, stopId, directionId, patterns, atTime, global)

    @Serializable
    data class ActivePeriod(val start: Instant, val end: Instant?) {
        // This is only nullable because it's set after serialization within the Alert init,
        // in practice it should always be populated with a value, unless something has gone wrong.
        var durationCertainty: DurationCertainty? = null

        fun activeAt(instant: Instant): Boolean {
            if (end == null) {
                return start <= instant
            }
            return instant in start..end
        }

        val startServiceDate = start.toBostonTime().serviceDate
        val endServiceDate = end?.toBostonTime()?.serviceDate(ServiceDateRounding.BACKWARDS)

        val fromStartOfService: Boolean
            get() {
                val localTime = start.toBostonTime()
                return localTime.hour == 3 && localTime.minute == 0
            }

        val toEndOfService: Boolean
            get() {
                val end = end ?: return false
                val localTime = end.toBostonTime()
                return (localTime.hour == 3 && localTime.minute == 0) ||
                    (localTime.hour == 2 && localTime.minute == 59)
            }

        val endingLaterToday: Boolean
            get() = durationCertainty == Alert.DurationCertainty.Estimated
    }

    @Serializable
    enum class Cause {
        @SerialName("accident") Accident,
        @SerialName("amtrak") Amtrak,
        @SerialName("amtrak_train_traffic") AmtrakTrainTraffic,
        @SerialName("an_earlier_mechanical_problem") AnEarlierMechanicalProblem,
        @SerialName("an_earlier_signal_problem") AnEarlierSignalProblem,
        @SerialName("autos_impeding_service") AutosImpedingService,
        @SerialName("coast_guard_restriction") CoastGuardRestriction,
        @SerialName("congestion") Congestion,
        @SerialName("construction") Construction,
        @SerialName("crossing_issue") CrossingIssue,
        @SerialName("crossing_malfunction") CrossingMalfunction,
        @SerialName("demonstration") Demonstration,
        @SerialName("disabled_bus") DisabledBus,
        @SerialName("disabled_train") DisabledTrain,
        @SerialName("drawbridge_being_raised") DrawbridgeBeingRaised,
        @SerialName("electrical_work") ElectricalWork,
        @SerialName("fire") Fire,
        @SerialName("fire_department_activity") FireDepartmentActivity,
        @SerialName("flooding") Flooding,
        @SerialName("fog") Fog,
        @SerialName("freight_train_interference") FreightTrainInterference,
        @SerialName("hazmat_condition") HazmatCondition,
        @SerialName("heavy_ridership") HeavyRidership,
        @SerialName("high_winds") HighWinds,
        @SerialName("holiday") Holiday,
        @SerialName("hurricane") Hurricane,
        @SerialName("ice_in_harbor") IceInHarbor,
        @SerialName("maintenance") Maintenance,
        @SerialName("mechanical_issue") MechanicalIssue,
        @SerialName("mechanical_problem") MechanicalProblem,
        @SerialName("medical_emergency") MedicalEmergency,
        @SerialName("other_cause") OtherCause,
        @SerialName("parade") Parade,
        @SerialName("police_action") PoliceAction,
        @SerialName("police_activity") PoliceActivity,
        @SerialName("power_problem") PowerProblem,
        @SerialName("rail_defect") RailDefect,
        @SerialName("severe_weather") SevereWeather,
        @SerialName("signal_issue") SignalIssue,
        @SerialName("signal_problem") SignalProblem,
        @SerialName("single_tracking") SingleTracking,
        @SerialName("slippery_rail") SlipperyRail,
        @SerialName("snow") Snow,
        @SerialName("special_event") SpecialEvent,
        @SerialName("speed_restriction") SpeedRestriction,
        @SerialName("strike") Strike,
        @SerialName("switch_issue") SwitchIssue,
        @SerialName("switch_problem") SwitchProblem,
        @SerialName("technical_problem") TechnicalProblem,
        @SerialName("tie_replacement") TieReplacement,
        @SerialName("track_problem") TrackProblem,
        @SerialName("track_work") TrackWork,
        @SerialName("traffic") Traffic,
        @SerialName("train_traffic") TrainTraffic,
        @SerialName("unruly_passenger") UnrulyPassenger,
        @SerialName("unknown_cause") UnknownCause,
        @SerialName("weather") Weather,
    }

    @Serializable
    enum class DurationCertainty {
        @SerialName("estimated") Estimated,
        @SerialName("known") Known,
        @SerialName("unknown") Unknown,
    }

    @Serializable
    enum class Effect {
        @SerialName("access_issue") AccessIssue,
        @SerialName("additional_service") AdditionalService,
        @SerialName("amber_alert") AmberAlert,
        @SerialName("bike_issue") BikeIssue,
        @SerialName("cancellation") Cancellation,
        @SerialName("delay") Delay,
        @SerialName("detour") Detour,
        @SerialName("dock_closure") DockClosure,
        @SerialName("dock_issue") DockIssue,
        @SerialName("elevator_closure") ElevatorClosure,
        @SerialName("escalator_closure") EscalatorClosure,
        @SerialName("extra_service") ExtraService,
        @SerialName("facility_issue") FacilityIssue,
        @SerialName("modified_service") ModifiedService,
        @SerialName("no_service") NoService,
        @SerialName("other_effect") OtherEffect,
        @SerialName("parking_closure") ParkingClosure,
        @SerialName("parking_issue") ParkingIssue,
        @SerialName("policy_change") PolicyChange,
        @SerialName("schedule_change") ScheduleChange,
        @SerialName("service_change") ServiceChange,
        @SerialName("shuttle") Shuttle,
        @SerialName("snow_route") SnowRoute,
        @SerialName("station_closure") StationClosure,
        @SerialName("station_issue") StationIssue,
        @SerialName("stop_closure") StopClosure,
        @SerialName("stop_move") StopMove,
        @SerialName("stop_moved") StopMoved,
        @SerialName("stop_shoveling") StopShoveling,
        @SerialName("summary") Summary,
        @SerialName("suspension") Suspension,
        @SerialName("track_change") TrackChange,
        @SerialName("unknown_effect") UnknownEffect,
    }

    @Serializable
    data class InformedEntity(
        val activities: List<Activity>,
        @SerialName("direction_id") val directionId: Int? = null,
        val facility: String? = null,
        val route: String? = null,
        @SerialName("route_type") val routeType: RouteType? = null,
        val stop: String? = null,
        val trip: String? = null,
    ) {
        @Serializable
        enum class Activity {
            @SerialName("board") Board,
            @SerialName("bringing_bike") BringingBike,
            @SerialName("exit") Exit,
            @SerialName("park_car") ParkCar,
            @SerialName("ride") Ride,
            @SerialName("store_bike") StoreBike,
            @SerialName("using_escalator") UsingEscalator,
            @SerialName("using_wheelchair") UsingWheelchair,
        }

        fun appliesTo(
            directionId: Int? = null,
            facilityId: String? = null,
            routeId: String? = null,
            routeType: RouteType? = null,
            stopId: String? = null,
            tripId: String? = null,
        ): Boolean {
            fun <T> matches(expected: T?, actual: T?) =
                expected == null || actual == null || expected == actual

            return matches(directionId, this.directionId) &&
                matches(facilityId, this.facility) &&
                matches(routeId, this.route) &&
                matches(routeType, this.routeType) &&
                matches(stopId, this.stop) &&
                matches(tripId, this.trip)
        }

        /** A more expressive way to represent a set of constraints than [appliesTo]. */
        inner class PredicateBuilder {
            var isSatisfied = true

            fun checkActivity(activity: Activity) {
                if (!isSatisfied) return
                if (activity !in this@InformedEntity.activities) {
                    isSatisfied = false
                }
            }

            fun checkActivityIn(vararg activities: Activity) = checkActivityIn(activities.toList())

            fun checkActivityIn(activities: Collection<Activity>) {
                if (!isSatisfied) return
                if (!this@InformedEntity.activities.any { it in activities }) {
                    isSatisfied = false
                }
            }

            fun checkDirection(directionId: Int?) {
                if (!isSatisfied) return
                if (directionId == null) return
                if (this@InformedEntity.directionId == null) return
                if (this@InformedEntity.directionId != directionId) {
                    isSatisfied = false
                }
            }

            fun checkRoute(routeId: String?) {
                if (!isSatisfied) return
                if (routeId == null) return
                if (this@InformedEntity.route == null) return
                if (this@InformedEntity.route != routeId) {
                    isSatisfied = false
                }
            }

            fun checkRouteIn(routeIds: Collection<String>) {
                if (!isSatisfied) return
                if (this@InformedEntity.route == null) return
                if (this@InformedEntity.route !in routeIds) {
                    isSatisfied = false
                }
            }

            fun checkStop(stopId: String?) {
                if (!isSatisfied) return
                if (stopId == null) return
                if (this@InformedEntity.stop == null) return
                if (this@InformedEntity.stop != stopId) {
                    isSatisfied = false
                }
            }

            fun checkStopIn(stopIds: Collection<String>) {
                if (!isSatisfied) return
                if (this@InformedEntity.stop == null) return
                if (this@InformedEntity.stop !in stopIds) {
                    isSatisfied = false
                }
            }

            fun checkTrip(tripId: String?) {
                if (!isSatisfied) return
                if (tripId == null) return
                if (this@InformedEntity.trip == null) return
                if (this@InformedEntity.trip != tripId) {
                    isSatisfied = false
                }
            }
        }

        fun satisfies(block: PredicateBuilder.() -> Unit): Boolean {
            val builder = PredicateBuilder()
            builder.block()
            return builder.isSatisfied
        }
    }

    @Serializable
    enum class Lifecycle {
        @SerialName("new") New,
        @SerialName("ongoing") Ongoing,
        @SerialName("ongoing_upcoming") OngoingUpcoming,
        @SerialName("upcoming") Upcoming,
    }

    fun currentPeriod(time: Instant) = activePeriod.firstOrNull { it.activeAt(time) }

    fun isActive(time: Instant) = currentPeriod(time) != null

    fun anyInformedEntity(predicate: (InformedEntity) -> Boolean) = informedEntity.any(predicate)

    fun anyInformedEntitySatisfies(predicateBuilder: InformedEntity.PredicateBuilder.() -> Unit) =
        informedEntity.any { it.satisfies(predicateBuilder) }

    fun matchingEntities(predicate: (InformedEntity) -> Boolean) = informedEntity.filter(predicate)

    companion object {
        /**
         * Returns alerts that are applicable to the passed in routes, stops, and trips
         *
         * Criteria:
         * - Route ID matches an alert [Alert.InformedEntity]
         * - Stop ID matches an alert [Alert.InformedEntity]
         * - Alert's informed entity activities contains [Alert.InformedEntity.Activity.Board]
         * - Trip ID matches an alert [Alert.InformedEntity]
         */
        fun applicableAlerts(
            alerts: Collection<Alert>,
            directionId: Int?,
            routeIds: List<String>,
            stopIds: Set<String>?,
            tripId: String?,
        ): List<Alert> {
            return alerts
                .filter { alert ->
                    alert.anyInformedEntitySatisfies {
                        checkActivity(InformedEntity.Activity.Board)
                        checkDirection(directionId)
                        checkRouteIn(routeIds)
                        if (stopIds != null) {
                            checkStopIn(stopIds)
                        }
                        checkTrip(tripId)
                    }
                }
                .distinct()
        }

        /**
         * Returns elevator alerts that are applicable to the passed in stops
         *
         * Criteria:
         * - Alert effect is [Alert.Effect.ElevatorClosure]
         * - Stop ID matches an alert [Alert.InformedEntity]
         * - Alert's informed entity activities contains
         *   [Alert.InformedEntity.Activity.UsingWheelchair]
         */
        fun elevatorAlerts(alerts: Collection<Alert>, stopIds: Set<String>): List<Alert> {
            return alerts
                .filter {
                    it.effect == Alert.Effect.ElevatorClosure &&
                        it.anyInformedEntity { entity ->
                            entity.activities.contains(
                                Alert.InformedEntity.Activity.UsingWheelchair
                            ) && stopIds.any { stopId -> entity.appliesTo(stopId = stopId) }
                        }
                }
                .distinct()
        }

        /**
         * Gets the alerts of the first stop that is downstream of the target stop which has any
         * alerts that are different from the alerts at the target stop. Considers only alerts that
         * have specified stops.
         *
         * @param alerts: The full list of alerts
         * @param trip: The trip used to calculate downstream stops
         * @param targetStopWithChildren: The child and parent stop Ids of the target stop
         */
        fun downstreamAlerts(
            alerts: Collection<Alert>,
            trip: Trip,
            targetStopWithChildren: Set<String>,
        ): List<Alert> {
            val stopIds = trip.stopIds ?: emptyList()

            val alerts =
                alerts.filter {
                    it.hasStopsSpecified && it.significance >= AlertSignificance.Accessibility
                }

            val targetStopAlertIds =
                alerts
                    .filter {
                        it.anyInformedEntitySatisfies {
                            checkActivityIn(
                                InformedEntity.Activity.Exit,
                                InformedEntity.Activity.Ride,
                            )
                            checkDirection(trip.directionId)
                            checkRoute(trip.routeId)
                            checkStopIn(targetStopWithChildren)
                        }
                    }
                    .map { it.id }
                    .toSet()

            val indexOfTargetStopInPattern =
                stopIds.indexOfFirst { targetStopWithChildren.contains(it) }
            if (indexOfTargetStopInPattern != -1 && indexOfTargetStopInPattern < stopIds.size - 1) {
                val downstreamStops = stopIds.subList(indexOfTargetStopInPattern + 1, stopIds.size)
                val firstStopAlerts =
                    downstreamStops
                        .map { stop ->
                            alerts.filter {
                                it.anyInformedEntitySatisfies {
                                    checkActivityIn(
                                        InformedEntity.Activity.Exit,
                                        InformedEntity.Activity.Ride,
                                    )
                                    checkDirection(trip.directionId)
                                    checkRoute(trip.routeId)
                                    checkStop(stop)
                                } && !targetStopAlertIds.contains(it.id)
                            }
                        }
                        .firstOrNull { it.isNotEmpty() } ?: listOf()
                return firstStopAlerts
            } else {
                return listOf()
            }
        }

        /**
         * A unique list of all the alerts that are downstream from the target stop for each route
         * pattern
         */
        fun alertsDownstreamForPatterns(
            alerts: Collection<Alert>,
            patterns: List<RoutePattern>,
            targetStopWithChildren: Set<String>,
            tripsById: Map<String, Trip>,
        ): List<Alert> {
            return patterns
                .flatMap {
                    val trip = tripsById[it.representativeTripId]
                    if (trip != null) {
                        downstreamAlerts(alerts, trip, targetStopWithChildren)
                    } else {
                        listOf()
                    }
                }
                .distinct()
        }
    }
}

fun List<Alert>.discardTrackChangesAtCRCore(isCRCore: Boolean): List<Alert> =
    if (isCRCore) this.filterNot { it.effect == Alert.Effect.TrackChange } else this
