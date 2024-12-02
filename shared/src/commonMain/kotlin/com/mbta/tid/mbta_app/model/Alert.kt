package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    override val id: String,
    @SerialName("active_period") val activePeriod: List<ActivePeriod>,
    val cause: Cause?,
    val description: String?,
    val effect: Effect,
    @SerialName("effect_name") val effectName: String?,
    val header: String?,
    @SerialName("informed_entity") val informedEntity: List<InformedEntity>,
    val lifecycle: Lifecycle,
    @SerialName("updated_at") val updatedAt: Instant
) : BackendObject {
    val alertState: StopAlertState =
        when (this.effect) {
            Effect.Shuttle -> StopAlertState.Shuttle
            in setOf(
                Effect.Suspension,
                Effect.StationClosure,
                Effect.StopClosure,
                Effect.DockClosure
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
                Effect.SnowRoute
            ) -> if (hasStopsSpecified) AlertSignificance.Major else AlertSignificance.Secondary
            // service changes are always secondary
            Effect.ServiceChange -> AlertSignificance.Secondary
            else -> AlertSignificance.None
        }

    @Serializable
    data class ActivePeriod(val start: Instant, val end: Instant?) {
        fun activeAt(instant: Instant): Boolean {
            if (end == null) {
                return start <= instant
            }
            return instant in start..end
        }
    }

    @Serializable
    enum class Cause {
        @SerialName("accident") Accident,
        @SerialName("amtrak") Amtrak,
        @SerialName("an_earlier_mechanical_problem") AnEarlierMechanicalProblem,
        @SerialName("an_earlier_signal_problem") AnEarlierSignalProblem,
        @SerialName("autos_impeding_service") AutosImpedingService,
        @SerialName("coast_guard_restriction") CoastGuardRestriction,
        @SerialName("congestion") Congestion,
        @SerialName("construction") Construction,
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
        @SerialName("mechanical_problem") MechanicalProblem,
        @SerialName("medical_emergency") MedicalEmergency,
        @SerialName("other_cause") OtherCause,
        @SerialName("parade") Parade,
        @SerialName("police_action") PoliceAction,
        @SerialName("police_activity") PoliceActivity,
        @SerialName("power_problem") PowerProblem,
        @SerialName("severe_weather") SevereWeather,
        @SerialName("signal_problem") SignalProblem,
        @SerialName("slippery_rail") SlipperyRail,
        @SerialName("snow") Snow,
        @SerialName("special_event") SpecialEvent,
        @SerialName("speed_restriction") SpeedRestriction,
        @SerialName("strike") Strike,
        @SerialName("switch_problem") SwitchProblem,
        @SerialName("technical_problem") TechnicalProblem,
        @SerialName("tie_replacement") TieReplacement,
        @SerialName("track_problem") TrackProblem,
        @SerialName("track_work") TrackWork,
        @SerialName("traffic") Traffic,
        @SerialName("unruly_passenger") UnrulyPassenger,
        @SerialName("unknown_cause") UnknownCause,
        @SerialName("weather") Weather,
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
        val trip: String? = null
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
            tripId: String? = null
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
    }

    @Serializable
    enum class Lifecycle {
        @SerialName("new") New,
        @SerialName("ongoing") Ongoing,
        @SerialName("ongoing_upcoming") OngoingUpcoming,
        @SerialName("upcoming") Upcoming,
    }

    fun isActive(time: Instant) =
        activePeriod.any { it.start <= time && (it.end == null || it.end >= time) }

    fun anyInformedEntity(predicate: (InformedEntity) -> Boolean) = informedEntity.any(predicate)

    fun matchingEntities(predicate: (InformedEntity) -> Boolean) = informedEntity.filter(predicate)

    companion object {
        /**
         * Returns alerts that are applicable to the passed in routes and stops
         *
         * Criteria:
         * - Route ID matches an alert [Alert.InformedEntity]
         * - Stop ID matches an alert [Alert.InformedEntity]
         * - Alert's informed entity activities contains [Alert.InformedEntity.Activity.Board]
         */
        fun applicableAlerts(
            alerts: Collection<Alert>,
            directionId: Int?,
            routeIds: List<String>,
            stopIds: Set<String>?
        ): List<Alert> {
            return alerts
                .filter { alert ->
                    alert.anyInformedEntity {
                        routeIds.any { routeId ->
                            stopIds?.any { stopId ->
                                it.appliesTo(
                                    directionId = directionId,
                                    routeId = routeId,
                                    stopId = stopId
                                )
                            }
                                ?: it.appliesTo(directionId = directionId, routeId = routeId)
                        } && it.activities.contains(Alert.InformedEntity.Activity.Board)
                    }
                }
                .distinct()
        }

        /**
         * Gets the alerts of the first stop that is downstream of the target stop which has alerts.
         * Considers only alerts that have specified stops.
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

            val alerts = alerts.filter { it.hasStopsSpecified }

            val indexOfTargetStopInPattern =
                stopIds.indexOfFirst { targetStopWithChildren.contains(it) }
            if (indexOfTargetStopInPattern != -1 && indexOfTargetStopInPattern < stopIds.size - 1) {
                val downstreamStops = stopIds.subList(indexOfTargetStopInPattern + 1, stopIds.size)
                val firstStopAlerts =
                    downstreamStops
                        .map { stop ->
                            applicableAlerts(
                                alerts.toList() ?: listOf(),
                                trip.directionId,
                                listOf(trip.routeId),
                                setOf(stop)
                            )
                        }
                        .firstOrNull { alerts -> !alerts.isNullOrEmpty() }
                        ?: listOf()
                return firstStopAlerts
            } else {
                return listOf()
            }
        }
    }
}
