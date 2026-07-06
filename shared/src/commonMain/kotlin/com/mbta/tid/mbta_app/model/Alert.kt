package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RoutePattern.Typicality
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.ShouldRefineInSwift
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Alert
internal constructor(
    override val id: String,
    @SerialName("active_period") val activePeriod: List<ActivePeriod>,
    val cause: Cause? = Cause.UnknownCause,
    val description: String?,
    @SerialName("duration_certainty")
    val durationCertainty: DurationCertainty = DurationCertainty.Unknown,
    val effect: Effect = Effect.UnknownEffect,
    @SerialName("effect_name") val effectName: String?,
    val header: String?,
    @SerialName("informed_entity") val informedEntity: List<InformedEntity>,
    val lifecycle: Lifecycle,
    val severity: Int,
    @SerialName("updated_at") val updatedAt: EasternTimeInstant,
    // This field is not parsed from the Alert object from the backend, it is injected from
    // global data in the AlertsUsecase if any informed entities apply to a facility.
    val facilities: Map<String, Facility>? = null,
) : BackendObject<String> {
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

    val hasStopsSpecified: Boolean = informedEntity.all { it.stop != null }

    public fun allClear(atTime: EasternTimeInstant): Boolean = activePeriod.all {
        it.end != null && it.end < atTime
    }

    val stopSkipped: Boolean = effect.stopSkipped

    /**
     * If this alert has the given trip as an informed entity, return the significance of that trip
     * (same as the intrinsic significance, with the exception of treating cancellations as Major)
     *
     * if the alert doesn't specify the given trip, then returns null.
     */
    public fun tripSpecificSignificance(trip: String): AlertSignificance? {

        if (this.anyInformedEntity { it.trip == trip }) {
            if (effect == Effect.Cancellation) {
                return AlertSignificance.Major
            } else {
                return intrinsicSignificance
            }
        } else {
            return null
        }
    }

    /** The intrinsic significance of the alert, not considering the effect period. */
    public val intrinsicSignificance: AlertSignificance =
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
            // cancellation is major for the specific trip but minor for the
            // route/stop/direction
            Effect.Cancellation -> AlertSignificance.Minor
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

    public fun significance(atTime: EasternTimeInstant): AlertSignificance {
        val maxSignificance =
            when {
                // active now - use intrinsic significance
                isActive(atTime) -> AlertSignificance.Major
                // upcoming, show as secondary if will be major later
                willBeActiveSoon(atTime) -> AlertSignificance.Secondary
                // all clear
                allClear(atTime) -> AlertSignificance.Major
                // will be active later but not soon enough to show yet, hide completely
                else -> AlertSignificance.None
            }
        return minOf(intrinsicSignificance, maxSignificance)
    }

    val hasNoThroughService: Boolean = effect in setOf(Effect.Shuttle, Effect.Suspension)

    public fun summary(
        routeId: Matcher<Route.Id>,
        stopId: Matcher<String>,
        directionId: Matcher<Int>,
        tripId: Matcher<String>,
    ): AlertSummaryEntity? =
        AlertSummaryEntity.matching(summaries.orEmpty(), routeId, stopId, directionId, tripId)

    @Serializable
    public data class ActivePeriod
    internal constructor(val start: EasternTimeInstant, val end: EasternTimeInstant?) {
        // This is only nullable because it's set after serialization within the Alert init,
        // in practice it should always be populated with a value, unless something has gone wrong.
        var durationCertainty: DurationCertainty? = null

        internal fun activeAt(instant: EasternTimeInstant): Boolean {
            if (end == null) {
                return start <= instant
            }
            return instant in start..end
        }

        val startServiceDate: LocalDate = start.serviceDate
        val endServiceDate: LocalDate? =
            end?.serviceDate(EasternTimeInstant.ServiceDateRounding.BACKWARDS)

        val fromStartOfService: Boolean
            get() = start.local.hour == 3 && start.local.minute == 0

        val toEndOfService: Boolean
            get() {
                val end = end ?: return false
                return (end.local.hour == 3 && end.local.minute == 0) ||
                    (end.local.hour == 2 && end.local.minute == 59)
            }

        val endingLaterToday: Boolean
            get() = durationCertainty == DurationCertainty.Estimated
    }

    @Serializable
    public enum class Cause {
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
        @SerialName("drawbridge_issue") DrawbridgeIssue,
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
    public enum class DurationCertainty {
        @SerialName("estimated") Estimated,
        @SerialName("known") Known,
        @SerialName("unknown") Unknown,
    }

    @Serializable
    public enum class Effect {
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
        @SerialName("notice") Notice,
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
        @SerialName("unknown_effect") UnknownEffect;

        public val stopSkipped: Boolean
            get() = this in setOf(StationClosure, StopClosure)
    }

    @Serializable
    public data class InformedEntity
    internal constructor(
        val activities: List<Activity>,
        @SerialName("direction_id") val directionId: Int? = null,
        val facility: String? = null,
        val route: Route.Id? = null,
        @SerialName("route_type") val routeType: RouteType? = null,
        val stop: String? = null,
        val trip: String? = null,
    ) {
        @Serializable
        public enum class Activity {
            @SerialName("board") Board,
            @SerialName("bringing_bike") BringingBike,
            @SerialName("exit") Exit,
            @SerialName("park_car") ParkCar,
            @SerialName("ride") Ride,
            @SerialName("store_bike") StoreBike,
            @SerialName("using_escalator") UsingEscalator,
            @SerialName("using_wheelchair") UsingWheelchair,
        }

        internal fun matches(
            activity: Matcher<Activity> = Matcher.Wildcard(),
            directionId: Matcher<Int> = Matcher.Wildcard(),
            facilityId: Matcher<String> = Matcher.Wildcard(),
            routeId: Matcher<Route.Id> = Matcher.Wildcard(),
            routeType: Matcher<RouteType> = Matcher.Wildcard(),
            stopId: Matcher<String> = Matcher.Wildcard(),
            tripId: Matcher<String> = Matcher.Wildcard(),
        ): Boolean {
            fun <T : Any> matches(expected: Matcher<T>, actual: T?): Boolean =
                actual == null || expected.matches(actual)

            return activities.any { matches(activity, it) } &&
                matches(directionId, this.directionId) &&
                matches(facilityId, this.facility) &&
                matches(routeId, this.route) &&
                matches(routeType, this.routeType) &&
                matches(stopId, this.stop) &&
                matches(tripId, this.trip)
        }
    }

    @Serializable
    public enum class Lifecycle {
        @SerialName("new") New,
        @SerialName("ongoing") Ongoing,
        @SerialName("ongoing_upcoming") OngoingUpcoming,
        @SerialName("upcoming") Upcoming,
    }

    public fun currentPeriod(time: EasternTimeInstant): ActivePeriod? = activePeriod.firstOrNull {
        it.activeAt(time)
    }

    public fun currentOrNextPeriod(now: EasternTimeInstant): ActivePeriod? =
        activePeriod.firstOrNull {
            it.end?.instant?.let { end -> end < now.instant } ?: true
        }

    /**
     * Gets an active period which is not currently active but which will start in the next 24
     * hours.
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @ShouldRefineInSwift
    public fun nextPeriod(time: EasternTimeInstant, within: Duration = 24.hours): ActivePeriod? =
        activePeriod.firstOrNull {
            it.start > time && (within == Duration.INFINITE || it.start <= time + within)
        }

    internal fun isActive(time: EasternTimeInstant) = currentPeriod(time) != null

    internal fun willBeActiveSoon(time: EasternTimeInstant) = nextPeriod(time) != null

    internal fun anyInformedEntity(predicate: (InformedEntity) -> Boolean) =
        informedEntity.any(predicate)

    internal fun anyInformedEntityMatches(
        activity: Matcher<InformedEntity.Activity> = Matcher.Wildcard(),
        directionId: Matcher<Int> = Matcher.Wildcard(),
        facilityId: Matcher<String> = Matcher.Wildcard(),
        routeId: Matcher<Route.Id> = Matcher.Wildcard(),
        routeType: Matcher<RouteType> = Matcher.Wildcard(),
        stopId: Matcher<String> = Matcher.Wildcard(),
        tripId: Matcher<String> = Matcher.Wildcard(),
    ) = informedEntity.any {
        it.matches(activity, directionId, facilityId, routeId, routeType, stopId, tripId)
    }

    internal fun matchingEntities(predicate: (InformedEntity) -> Boolean) =
        informedEntity.filter(predicate)

    public data class RecurrenceInfo(
        val start: EasternTimeInstant,
        val end: EasternTimeInstant,
        val days: Set<DayOfWeek>,
        val endDayKnown: Boolean,
    ) {
        public val daily: Boolean =
            days ==
                (start.serviceDate..end.serviceDate(
                            EasternTimeInstant.ServiceDateRounding.BACKWARDS
                        ))
                    .map { it.dayOfWeek }
                    .toSet() && days.count() > 1

        public val isWeekdays: Boolean =
            days ==
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                )
        public val isWeekends: Boolean = days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        public val fromStartOfService: Boolean = start.local.time == LocalTime(3, 0)
        public val toEndOfService: Boolean =
            end.local.time == LocalTime(2, 59) || end.local.time == LocalTime(3, 0)
    }

    public fun recurrenceRange(): RecurrenceInfo? {
        if (activePeriod.size <= 1) return null
        val firstPeriod = activePeriod.minBy { it.start }
        val lastPeriod = activePeriod.maxBy { it.end ?: return@recurrenceRange null }
        if (
            lastPeriod.endServiceDate != null &&
                firstPeriod.startServiceDate == lastPeriod.endServiceDate
        ) {
            return null
        }
        val lastPeriodEnd = lastPeriod.end ?: return null
        val seenDaysOfWeek =
            activePeriod
                .flatMap {
                    if (it.endServiceDate == null) {
                        DayOfWeek.entries.toSet()
                    } else {
                        (it.startServiceDate..it.endServiceDate).map { it.dayOfWeek }.toSet()
                    }
                }
                .sorted()
                .toSet()

        return RecurrenceInfo(
            firstPeriod.start,
            lastPeriodEnd,
            seenDaysOfWeek,
            endDayKnown = durationCertainty == DurationCertainty.Known,
        )
    }

    internal companion object {
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
            directionId: Int,
            routeIds: List<Route.Id>,
            routeType: RouteType,
            stopIds: Set<String>?,
            tripId: String?,
        ): List<Alert> {
            return alerts
                .filter { alert ->
                    alert.anyInformedEntityMatches(
                        activity = Matcher.Data(InformedEntity.Activity.Board),
                        directionId = Matcher.Data(directionId),
                        routeId = Matcher.AnyOf(routeIds),
                        routeType = Matcher.Data(routeType),
                        stopId = stopIds?.let { Matcher.AnyOf(it) } ?: Matcher.Wildcard(),
                        tripId = tripId?.let { Matcher.Data(it) } ?: Matcher.Wildcard(),
                    )
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
                    it.effect == Effect.ElevatorClosure &&
                        it.anyInformedEntityMatches(
                            activity = Matcher.Data(InformedEntity.Activity.UsingWheelchair),
                            stopId = Matcher.AnyOf(stopIds),
                        )
                }
                .distinct()
        }

        /**
         * Gets the alerts of the stops that are downstream of the target stop which have alerts up
         * to the first suspension or shuttle that are different from the alerts at the target stop.
         * Considers only alerts that have specified stops.
         *
         * @param alerts: The full list of alerts
         * @param trip: The trip used to calculate downstream stops
         * @param targetStopWithChildren: The child and parent stop Ids of the target stop
         */
        fun downstreamAlerts(
            alerts: Collection<Alert>,
            trip: Trip,
            routeType: RouteType,
            targetStopWithChildren: Set<String>,
        ): List<Alert> {
            val stopIds = trip.stopIds ?: emptyList()
            // find the index of the first stop id in target stop with children
            val indexOfTargetStopInPattern = stopIds.indexOfFirst {
                targetStopWithChildren.contains(it)
            }
            // Return empty if target stop with children is not on stopIds
            if (indexOfTargetStopInPattern < 0) return listOf()
            // Return empty if target stop with children is the last stop id
            if (indexOfTargetStopInPattern == stopIds.size - 1) return listOf()

            // Filter out alerts without stop specified and lower than accessibility significance
            val alerts = alerts.filter {
                it.hasStopsSpecified && it.intrinsicSignificance >= AlertSignificance.Accessibility
            }

            val relevantAlerts =
                alerts
                    .filter {
                        it.anyInformedEntityMatches(
                            activity =
                                Matcher.AnyOf(
                                    InformedEntity.Activity.Exit,
                                    InformedEntity.Activity.Ride,
                                ),
                            directionId = Matcher.Data(trip.directionId),
                            routeId = Matcher.Data(trip.routeId),
                            routeType = Matcher.Data(routeType),
                        )
                    }
                    .toList()

            // obtaining the target stop alert ids
            val targetStopAlertIds =
                relevantAlerts
                    .filter {
                        it.anyInformedEntityMatches(stopId = Matcher.AnyOf(targetStopWithChildren))
                    }
                    .map { it.id }
                    .toSet()

            val downstreamStops = stopIds.subList(indexOfTargetStopInPattern + 1, stopIds.size)
            val tripAlerts = downstreamStops.flatMap { stop ->
                relevantAlerts.filter {
                    it.anyInformedEntityMatches(stopId = Matcher.Data(stop)) &&
                        !targetStopAlertIds.contains(it.id)
                }
            }
            if (tripAlerts.isEmpty()) return emptyList()
            val lastIndex = tripAlerts.indexOfFirst {
                setOf(Effect.Suspension, Effect.Shuttle).contains(it.effect)
            }
            // If there is an alert with suspension or shuttle, only return alerts up to that point
            if (lastIndex >= 0) {
                return tripAlerts.subList(0, lastIndex + 1)
            }
            return tripAlerts
        }

        /**
         * A unique list of all the alerts that are downstream from the target stop for each route
         * pattern
         */
        fun alertsDownstreamForPatterns(
            alerts: Collection<Alert>,
            patterns: List<RoutePattern>,
            routeType: RouteType,
            targetStopWithChildren: Set<String>,
            tripsById: Map<String, Trip>,
        ): List<Alert> {
            return patterns
                .filterNot { it.typicality == Typicality.CanonicalOnly }
                .flatMap {
                    val trip = tripsById[it.representativeTripId]
                    if (trip != null) {
                        downstreamAlerts(alerts, trip, routeType, targetStopWithChildren)
                    } else {
                        listOf()
                    }
                }
                .distinct()
        }
    }
}

internal fun List<Alert>.discardTrackChangesAtCRCore(isCRCore: Boolean): List<Alert> =
    if (isCRCore) this.filterNot { it.effect == Alert.Effect.TrackChange } else this
