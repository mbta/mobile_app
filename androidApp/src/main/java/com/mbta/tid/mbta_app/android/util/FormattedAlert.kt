package com.mbta.tid.mbta_app.android.util

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.directionNameFormatted
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Facility

data class FormattedAlert(
    val alert: Alert?,
    val alertSummary: AlertSummary?,
    @StringRes val effectRes: Int,
    @StringRes val sentenceEffectRes: Int,
    @StringRes val causeRes: Int?,
    @StringRes val dueToCauseRes: Int?,
    /**
     * Represents the text and possible accessibility label that would be used if replacing
     * predictions. Does not guarantee that the alert should replace predictions.
     */
    val predictionReplacement: PredictionReplacement,
) {
    constructor(
        alert: Alert?,
        alertSummary: AlertSummary? = null,
    ) : this(
        alert,
        alertSummary,
        effectRes(alert?.effect ?: alertSummary?.effect),
        sentenceEffectRes(alert?.effect ?: alertSummary?.effect),
        causeRes(alert?.cause),
        dueToCauseRes(alert?.cause),
        predictionReplacement(alert?.effect ?: alertSummary?.effect),
    )

    fun cause(resources: Resources) = causeRes?.let { resources.getString(it) }

    val cause
        @Composable get() = cause(LocalResources.current)

    fun effect(resources: Resources) =
        resources.getString(R.string.effect, resources.getString(effectRes))

    val effect
        @Composable get() = effect(LocalResources.current)

    private fun downstreamEffect(resources: Resources) =
        resources.getString(R.string.effect_ahead, resources.getString(sentenceEffectRes))

    private fun delaysDueToCause(resources: Resources) =
        dueToCauseRes?.let {
            resources.getString(R.string.delays_due_to_cause, resources.getString(it))
        } ?: resources.getString(R.string.delays_unknown_reason)

    private fun delayHeader(resources: Resources) =
        // Show "Single Tracking" if there is an informational delay alert with that cause
        // (Any other information severity delay alerts are never shown)
        cause(resources)?.let {
            if (alert?.cause == Alert.Cause.SingleTracking && alert.severity < 3) {
                AnnotatedString.fromHtml(resources.getString(R.string.effect, it))
            } else null
        } ?: AnnotatedString.fromHtml(delaysDueToCause(resources))

    private fun elevatorHeader(resources: Resources) =
        AnnotatedString(
            alert
                ?.informedEntity
                ?.mapNotNull { alert.facilities?.get(it.facility) }
                ?.filter { it.type == Facility.Type.Elevator }
                ?.distinct()
                ?.singleOrNull()
                ?.shortName
                ?.let { facilityName ->
                    resources.getString(R.string.alert_elevator_header, facilityName)
                } ?: alert?.header ?: effect(resources)
        )

    private fun summary(resources: Resources) =
        alertSummary?.let {
            AnnotatedString.fromHtml(
                resources.getString(
                    R.string.alert_summary,
                    resources.getString(sentenceEffectRes),
                    summaryLocation(resources),
                    summaryTimeframe(resources),
                )
            )
        }

    fun alertCardHeader(spec: AlertCardSpec, resources: Resources) =
        when (spec) {
            AlertCardSpec.Downstream ->
                summary(resources) ?: AnnotatedString.fromHtml(downstreamEffect(resources))
            AlertCardSpec.Elevator -> elevatorHeader(resources)
            AlertCardSpec.Delay -> delayHeader(resources)
            AlertCardSpec.Secondary ->
                summary(resources) ?: AnnotatedString.fromHtml(effect(resources))
            else -> AnnotatedString.fromHtml(effect(resources))
        }

    @Composable
    fun alertCardHeader(spec: AlertCardSpec) = alertCardHeader(spec, LocalResources.current)

    fun alertCardMajorBody(resources: Resources) =
        summary(resources) ?: AnnotatedString(alert?.header ?: "")

    val alertCardMajorBody
        @Composable get() = alertCardMajorBody(LocalResources.current)

    private fun summaryLocation(resources: Resources) =
        alertSummary?.location?.let {
            when (it) {
                is AlertSummary.Location.SingleStop ->
                    resources.getString(R.string.alert_summary_location_single, it.stopName)
                is AlertSummary.Location.SuccessiveStops ->
                    resources.getString(
                        R.string.alert_summary_location_successive,
                        it.startStopName,
                        it.endStopName,
                    )
                is AlertSummary.Location.StopToDirection ->
                    resources.getString(
                        R.string.alert_summary_location_stop_to_direction,
                        it.startStopName,
                        resources.getString(directionNameFormatted(it.direction)),
                    )
                is AlertSummary.Location.DirectionToStop ->
                    resources.getString(
                        R.string.alert_summary_location_direction_to_stop,
                        resources.getString(directionNameFormatted(it.direction)),
                        it.endStopName,
                    )
                AlertSummary.Location.Unknown -> null
            }
        } ?: ""

    private fun summaryTimeframe(resources: Resources) =
        alertSummary?.timeframe?.let {
            when (it) {
                AlertSummary.Timeframe.EndOfService ->
                    resources.getString(R.string.alert_summary_timeframe_end_of_service)
                AlertSummary.Timeframe.Tomorrow ->
                    resources.getString(R.string.alert_summary_timeframe_tomorrow)
                is AlertSummary.Timeframe.LaterDate ->
                    resources.getString(
                        R.string.alert_summary_timeframe_later_date,
                        it.time.formattedServiceDate(),
                    )
                is AlertSummary.Timeframe.ThisWeek ->
                    resources.getString(
                        R.string.alert_summary_timeframe_this_week,
                        it.time.formattedServiceDay(),
                    )
                is AlertSummary.Timeframe.Time ->
                    resources.getString(
                        R.string.alert_summary_timeframe_time,
                        it.time.formattedTime(),
                    )
                AlertSummary.Timeframe.Unknown -> null
            }
        } ?: ""

    data class PredictionReplacement(
        @StringRes val textRes: Int,
        @StringRes val contentDescriptionRes: Int? = null,
    ) {
        val text
            @Composable get() = stringResource(textRes)

        val contentDescription
            @Composable get() = contentDescriptionRes?.let { stringResource(it) }
    }

    companion object {
        @StringRes
        private fun effectRes(effect: Alert.Effect?) =
            when (effect) {
                Alert.Effect.AccessIssue -> R.string.access_issue
                Alert.Effect.AdditionalService -> R.string.additional_service
                Alert.Effect.AmberAlert -> R.string.amber_alert
                Alert.Effect.BikeIssue -> R.string.bike_issue
                Alert.Effect.Cancellation -> R.string.cancellation
                Alert.Effect.Delay -> R.string.delay
                Alert.Effect.Detour -> R.string.detour
                Alert.Effect.DockClosure -> R.string.dock_closure
                Alert.Effect.DockIssue -> R.string.dock_issue
                Alert.Effect.ElevatorClosure -> R.string.elevator_closure
                Alert.Effect.EscalatorClosure -> R.string.escalator_closure
                Alert.Effect.ExtraService -> R.string.extra_service
                Alert.Effect.FacilityIssue -> R.string.facility_issue
                Alert.Effect.ModifiedService -> R.string.modified_service
                Alert.Effect.NoService -> R.string.no_service
                Alert.Effect.ParkingClosure -> R.string.parking_closure
                Alert.Effect.ParkingIssue -> R.string.parking_issue
                Alert.Effect.PolicyChange -> R.string.policy_change
                Alert.Effect.ScheduleChange -> R.string.schedule_change
                Alert.Effect.ServiceChange -> R.string.service_change
                Alert.Effect.Shuttle -> R.string.shuttle
                Alert.Effect.SnowRoute -> R.string.snow_route
                Alert.Effect.StationClosure -> R.string.station_closure
                Alert.Effect.StationIssue -> R.string.station_issue
                Alert.Effect.StopClosure -> R.string.stop_closure
                Alert.Effect.StopMove,
                Alert.Effect.StopMoved -> R.string.stop_moved
                Alert.Effect.StopShoveling -> R.string.stop_shoveling
                Alert.Effect.Summary -> R.string.summary
                Alert.Effect.Suspension -> R.string.suspension
                Alert.Effect.TrackChange -> R.string.track_change
                Alert.Effect.OtherEffect,
                Alert.Effect.UnknownEffect,
                null -> R.string.alert
            }

        @StringRes
        private fun sentenceEffectRes(effect: Alert.Effect?) =
            when (effect) {
                Alert.Effect.AccessIssue -> R.string.access_issue_sentence_case
                Alert.Effect.AdditionalService -> R.string.additional_service_sentence_case
                Alert.Effect.AmberAlert -> R.string.amber_alert_sentence_case
                Alert.Effect.BikeIssue -> R.string.bike_issue_sentence_case
                Alert.Effect.Cancellation -> R.string.trip_cancelled
                Alert.Effect.Delay -> R.string.delay
                Alert.Effect.Detour -> R.string.detour
                Alert.Effect.DockClosure -> R.string.dock_closed_sentence_case
                Alert.Effect.DockIssue -> R.string.dock_issue_sentence_case
                Alert.Effect.ElevatorClosure -> R.string.elevator_closed_sentence_case
                Alert.Effect.EscalatorClosure -> R.string.escalator_closed_sentence_case
                Alert.Effect.ExtraService -> R.string.extra_service_sentence_case
                Alert.Effect.FacilityIssue -> R.string.facility_issue_sentence_case
                Alert.Effect.ModifiedService -> R.string.modified_service_sentence_case
                Alert.Effect.NoService -> R.string.no_service_sentence_case
                Alert.Effect.ParkingClosure -> R.string.parking_closed
                Alert.Effect.ParkingIssue -> R.string.parking_issue_sentence_case
                Alert.Effect.PolicyChange -> R.string.policy_change_sentence_case
                Alert.Effect.ScheduleChange -> R.string.schedule_change_sentence_case
                Alert.Effect.ServiceChange -> R.string.service_change_sentence_case
                Alert.Effect.Shuttle -> R.string.shuttle_buses
                Alert.Effect.SnowRoute -> R.string.snow_route_sentence_case
                Alert.Effect.StationClosure -> R.string.station_closed_sentence_case
                Alert.Effect.StationIssue -> R.string.station_issue_sentence_case
                Alert.Effect.StopClosure -> R.string.stop_closed_sentence_case
                Alert.Effect.StopMove,
                Alert.Effect.StopMoved -> R.string.stop_moved_sentence_case
                Alert.Effect.StopShoveling -> R.string.stop_shoveling_sentence_case
                Alert.Effect.Summary -> R.string.summary
                Alert.Effect.Suspension -> R.string.service_suspended
                Alert.Effect.TrackChange -> R.string.track_change_sentence_case
                Alert.Effect.OtherEffect,
                Alert.Effect.UnknownEffect,
                null -> R.string.alert
            }

        private fun causeRes(cause: Alert.Cause?) =
            when (cause) {
                Alert.Cause.Accident -> R.string.accident
                Alert.Cause.Amtrak -> R.string.amtrak
                Alert.Cause.AmtrakTrainTraffic -> R.string.amtrak_train_traffic
                Alert.Cause.AnEarlierMechanicalProblem -> R.string.an_earlier_mechanical_problem
                Alert.Cause.AnEarlierSignalProblem -> R.string.an_earlier_signal_problem
                Alert.Cause.AutosImpedingService -> R.string.autos_impeding_service
                Alert.Cause.CoastGuardRestriction -> R.string.coast_guard_restriction
                Alert.Cause.Congestion -> R.string.congestion
                Alert.Cause.Construction -> R.string.construction
                Alert.Cause.CrossingIssue -> R.string.crossing_issue
                Alert.Cause.CrossingMalfunction -> R.string.crossing_malfunction
                Alert.Cause.Demonstration -> R.string.demonstration
                Alert.Cause.DisabledBus -> R.string.disabled_bus
                Alert.Cause.DisabledTrain -> R.string.disabled_train
                Alert.Cause.DrawbridgeBeingRaised -> R.string.drawbridge_being_raised
                Alert.Cause.ElectricalWork -> R.string.electrical_work
                Alert.Cause.Fire -> R.string.fire
                Alert.Cause.FireDepartmentActivity -> R.string.fire_department_activity
                Alert.Cause.Flooding -> R.string.flooding
                Alert.Cause.Fog -> R.string.fog
                Alert.Cause.FreightTrainInterference -> R.string.freight_train_interference
                Alert.Cause.HazmatCondition -> R.string.hazmat_condition
                Alert.Cause.HeavyRidership -> R.string.heavy_ridership
                Alert.Cause.HighWinds -> R.string.high_winds
                Alert.Cause.Holiday -> R.string.holiday
                Alert.Cause.Hurricane -> R.string.hurricane
                Alert.Cause.IceInHarbor -> R.string.ice_in_harbor
                Alert.Cause.Maintenance -> R.string.maintenance
                Alert.Cause.MechanicalIssue -> R.string.mechanical_issue
                Alert.Cause.MechanicalProblem -> R.string.mechanical_problem
                Alert.Cause.MedicalEmergency -> R.string.medical_emergency
                Alert.Cause.Parade -> R.string.parade
                Alert.Cause.PoliceAction -> R.string.police_action
                Alert.Cause.PoliceActivity -> R.string.police_activity
                Alert.Cause.PowerProblem -> R.string.power_problem
                Alert.Cause.RailDefect -> R.string.rail_defect
                Alert.Cause.SevereWeather -> R.string.severe_weather
                Alert.Cause.SignalIssue -> R.string.signal_issue
                Alert.Cause.SignalProblem -> R.string.signal_problem
                Alert.Cause.SingleTracking -> R.string.single_tracking
                Alert.Cause.SlipperyRail -> R.string.slippery_rail
                Alert.Cause.Snow -> R.string.snow
                Alert.Cause.SpecialEvent -> R.string.special_event
                Alert.Cause.SpeedRestriction -> R.string.speed_restriction
                Alert.Cause.Strike -> R.string.strike
                Alert.Cause.SwitchIssue -> R.string.switch_issue
                Alert.Cause.SwitchProblem -> R.string.switch_problem
                Alert.Cause.TechnicalProblem -> R.string.technical_problem
                Alert.Cause.TieReplacement -> R.string.tie_replacement
                Alert.Cause.TrackProblem -> R.string.track_problem
                Alert.Cause.TrackWork -> R.string.track_work
                Alert.Cause.Traffic -> R.string.traffic
                Alert.Cause.TrainTraffic -> R.string.train_traffic
                Alert.Cause.UnrulyPassenger -> R.string.unruly_passenger
                Alert.Cause.Weather -> R.string.weather
                else -> null
            }

        @StringRes
        private fun dueToCauseRes(cause: Alert.Cause?) =
            when (cause) {
                Alert.Cause.Accident -> R.string.accident_lowercase
                Alert.Cause.Amtrak -> R.string.amtrak
                Alert.Cause.AmtrakTrainTraffic -> R.string.amtrak_train_traffic_lowercase
                Alert.Cause.AnEarlierMechanicalProblem ->
                    R.string.an_earlier_mechanical_problem_lowercase
                Alert.Cause.AnEarlierSignalProblem -> R.string.an_earlier_signal_problem_lowercase
                Alert.Cause.AutosImpedingService -> R.string.autos_impeding_service_lowercase
                Alert.Cause.CoastGuardRestriction -> R.string.coast_guard_restriction_lowercase
                Alert.Cause.Congestion -> R.string.congestion_lowercase
                Alert.Cause.Construction -> R.string.construction_lowercase
                Alert.Cause.CrossingIssue -> R.string.crossing_issue_lowercase
                Alert.Cause.CrossingMalfunction -> R.string.crossing_malfunction_lowercase
                Alert.Cause.Demonstration -> R.string.demonstration_lowercase
                Alert.Cause.DisabledBus -> R.string.disabled_bus_lowercase
                Alert.Cause.DisabledTrain -> R.string.disabled_train_lowercase
                Alert.Cause.DrawbridgeBeingRaised -> R.string.drawbridge_being_raised_lowercase
                Alert.Cause.ElectricalWork -> R.string.electrical_work_lowercase
                Alert.Cause.Fire -> R.string.fire_lowercase
                Alert.Cause.FireDepartmentActivity -> R.string.fire_department_activity_lowercase
                Alert.Cause.Flooding -> R.string.flooding_lowercase
                Alert.Cause.Fog -> R.string.fog_lowercase
                Alert.Cause.FreightTrainInterference ->
                    R.string.freight_train_interference_lowercase
                Alert.Cause.HazmatCondition -> R.string.hazmat_condition_lowercase
                Alert.Cause.HeavyRidership -> R.string.heavy_ridership_lowercase
                Alert.Cause.HighWinds -> R.string.high_winds_lowercase
                Alert.Cause.Holiday -> R.string.holiday_lowercase
                Alert.Cause.Hurricane -> R.string.hurricane_lowercase
                Alert.Cause.IceInHarbor -> R.string.ice_in_harbor_lowercase
                Alert.Cause.Maintenance -> R.string.maintenance_lowercase
                Alert.Cause.MechanicalIssue -> R.string.mechanical_issue_lowercase
                Alert.Cause.MechanicalProblem -> R.string.mechanical_problem_lowercase
                Alert.Cause.MedicalEmergency -> R.string.medical_emergency_lowercase
                Alert.Cause.Parade -> R.string.parade_lowercase
                Alert.Cause.PoliceAction -> R.string.police_action_lowercase
                Alert.Cause.PoliceActivity -> R.string.police_activity_lowercase
                Alert.Cause.PowerProblem -> R.string.power_problem_lowercase
                Alert.Cause.RailDefect -> R.string.rail_defect_lowercase
                Alert.Cause.SevereWeather -> R.string.severe_weather_lowercase
                Alert.Cause.SignalIssue -> R.string.signal_issue_lowercase
                Alert.Cause.SignalProblem -> R.string.signal_problem_lowercase
                Alert.Cause.SingleTracking -> R.string.single_tracking_lowercase
                Alert.Cause.SlipperyRail -> R.string.slippery_rail_lowercase
                Alert.Cause.Snow -> R.string.snow_lowercase
                Alert.Cause.SpecialEvent -> R.string.special_event_lowercase
                Alert.Cause.SpeedRestriction -> R.string.speed_restriction_lowercase
                Alert.Cause.Strike -> R.string.strike_lowercase
                Alert.Cause.SwitchIssue -> R.string.switch_issue_lowercase
                Alert.Cause.SwitchProblem -> R.string.switch_problem_lowercase
                Alert.Cause.TechnicalProblem -> R.string.technical_problem_lowercase
                Alert.Cause.TieReplacement -> R.string.tie_replacement_lowercase
                Alert.Cause.TrackProblem -> R.string.track_problem_lowercase
                Alert.Cause.TrackWork -> R.string.track_work_lowercase
                Alert.Cause.Traffic -> R.string.traffic_lowercase
                Alert.Cause.TrainTraffic -> R.string.train_traffic_lowercase
                Alert.Cause.UnrulyPassenger -> R.string.unruly_passenger_lowercase
                Alert.Cause.Weather -> R.string.weather_lowercase
                else -> null
            }

        private fun predictionReplacement(effect: Alert.Effect?) =
            when (effect) {
                Alert.Effect.DockClosure -> PredictionReplacement(R.string.dock_closed)
                Alert.Effect.Shuttle ->
                    PredictionReplacement(
                        R.string.shuttle_bus,
                        R.string.shuttle_buses_replace_service,
                    )
                Alert.Effect.StationClosure -> PredictionReplacement(R.string.station_closed)
                Alert.Effect.StopClosure -> PredictionReplacement(R.string.stop_closed)
                Alert.Effect.Suspension ->
                    PredictionReplacement(R.string.suspension, R.string.service_suspended)
                else -> PredictionReplacement(effectRes(effect))
            }
    }
}
