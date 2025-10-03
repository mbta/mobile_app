package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.utils.SharedString

val SharedString.value: String
    @Composable
    get() =
        when (this) {
            SharedString.CommuterRailAndFerryTickets ->
                stringResource(R.string.resources_link_mticket)
            SharedString.DebugMode -> stringResource(R.string.feature_flag_debug_mode)
            SharedString.FareInformation -> stringResource(R.string.resources_link_fare_info)
            SharedString.FeatureFlagsSection -> stringResource(R.string.more_section_feature_flags)
            SharedString.MapDisplay -> stringResource(R.string.setting_toggle_map_display)
            SharedString.MTicketApp -> stringResource(R.string.resources_link_mticket_note)
            SharedString.Notifications -> "Notifications" // Temp feature flag
            SharedString.PrivacyPolicy -> stringResource(R.string.other_link_privacy_policy)
            SharedString.ResourcesSection -> stringResource(R.string.more_section_resources)
            SharedString.RouteSearch -> stringResource(R.string.feature_flag_route_search)
            SharedString.SendAppFeedback -> stringResource(R.string.feedback_link_form)
            SharedString.SettingsSection -> stringResource(R.string.more_section_settings)
            SharedString.SoftwareLicenses -> stringResource(R.string.software_licenses)
            SharedString.StationAccessibilityInfo ->
                stringResource(R.string.setting_station_accessibility)
            SharedString.SupportAccessibilityNote ->
                stringResource(R.string.more_section_support_note_accessibility)
            SharedString.SupportHours -> stringResource(R.string.more_section_support_note_hours)
            SharedString.SupportSection -> stringResource(R.string.more_section_support)
            SharedString.TermsOfUse -> stringResource(R.string.other_link_tos)
            SharedString.TripPlanner -> stringResource(R.string.resources_link_trip_planner)
            SharedString.ViewSourceOnGithub -> stringResource(R.string.other_link_source_code)
        }
