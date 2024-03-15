package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    override val id: String,
    @SerialName("active_period") val activePeriod: List<ActivePeriod>,
    val effect: Effect,
    @SerialName("effect_name") val effectName: String?,
    @SerialName("informed_entity") val informedEntity: List<InformedEntity>,
    val lifecycle: Lifecycle
) : BackendObject {
    @Serializable data class ActivePeriod(val start: Instant, val end: Instant?)

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
    }

    @Serializable
    enum class Lifecycle {
        @SerialName("new") New,
        @SerialName("ongoing") Ongoing,
        @SerialName("ongoing_upcoming") OngoingUpcoming,
        @SerialName("upcoming") Upcoming,
    }
}
