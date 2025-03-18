package com.mbta.tid.mbta_app.android.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Alert

data class FormattedAlert(
    @StringRes val effectRes: Int,
    @StringRes val downstreamEffectRes: Int,
    @StringRes val causeRes: Int?,
    /**
     * Represents the text and possible accessibility label that would be used if replacing
     * predictions. Does not guarantee that the alert should replace predictions.
     */
    val predictionReplacement: PredictionReplacement
) {
    constructor(
        alert: Alert
    ) : this(
        effectRes(alert.effect),
        downstreamEffectRes(alert.effect),
        causeRes(alert.cause),
        predictionReplacement(alert.effect),
    )

    val effect
        @Composable get() = stringResource(effectRes)

    val downstreamEffect
        @Composable
        get() = stringResource(R.string.effect_ahead, stringResource(downstreamEffectRes))

    // TODO: Decide if should be nullable or not
    val cause
        @Composable get() = causeRes?.let { stringResource(it) }

    data class PredictionReplacement(
        @StringRes val textRes: Int,
        @StringRes val contentDescriptionRes: Int? = null
    ) {
        val text
            @Composable get() = stringResource(textRes)

        val contentDescription
            @Composable get() = contentDescriptionRes?.let { stringResource(it) }
    }

    companion object {
        @StringRes
        private fun effectRes(effect: Alert.Effect) =
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
                Alert.Effect.UnknownEffect -> R.string.alert
            }

        @StringRes
        private fun downstreamEffectRes(effect: Alert.Effect) =
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
                Alert.Effect.UnknownEffect -> R.string.alert
            }

        @StringRes
        private fun causeRes(cause: Alert.Cause) =
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

        private fun predictionReplacement(effect: Alert.Effect) =
            when (effect) {
                Alert.Effect.DockClosure -> PredictionReplacement(R.string.dock_closed)
                Alert.Effect.Shuttle ->
                    PredictionReplacement(
                        R.string.shuttle_bus,
                        R.string.shuttle_buses_replace_service
                    )
                Alert.Effect.StationClosure -> PredictionReplacement(R.string.station_closed)
                Alert.Effect.StopClosure -> PredictionReplacement(R.string.stop_closed)
                Alert.Effect.Suspension ->
                    PredictionReplacement(R.string.suspension, R.string.service_suspended)
                else -> PredictionReplacement(effectRes(effect))
            }
    }
}
