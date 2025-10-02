package com.mbta.tid.mbta_app.viewModel

import com.mbta.tid.mbta_app.PlatformType
import com.mbta.tid.mbta_app.getPlatform
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.model.morePage.localizedFeedbackFormUrl
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.SharedString

public class MoreViewModel {

    public fun getSections(translation: String, licensesCallback: () -> Unit): List<MoreSection> {
        val platform = getPlatform()
        val feedbackFormUrl =
            when (platform.type) {
                PlatformType.iOS -> {
                    localizedFeedbackFormUrl("https://mbta.com/appfeedback", translation, true)
                }
                PlatformType.Android,
                PlatformType.JVM -> {
                    localizedFeedbackFormUrl("https://mbta.com/androidappfeedback", translation)
                }
            }
        val mTicketURL =
            when (platform.type) {
                PlatformType.iOS -> "https://apps.apple.com/us/app/mbta-mticket/id560487958"
                PlatformType.Android,
                PlatformType.JVM ->
                    "https://play.google.com/store/apps/details?id=com.mbta.mobileapp"
            }
        return listOf(
            MoreSection(
                id = MoreSection.Category.Feedback,
                items =
                    listOf(
                        MoreItem.Link(label = SharedString.SendAppFeedback, url = feedbackFormUrl)
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Resources,
                label = SharedString.ResourcesSection,
                items =
                    listOf(
                        MoreItem.Link(
                            label = SharedString.TripPlanner,
                            url = "https://www.mbta.com/trip-planner",
                        ),
                        MoreItem.Link(
                            label = SharedString.FareInformation,
                            url = "https://www.mbta.com/fares",
                        ),
                        MoreItem.Link(
                            label = SharedString.CommuterRailAndFerryTickets,
                            note = SharedString.MTicketApp,
                            url = mTicketURL,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Settings,
                label = SharedString.SettingsSection,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = SharedString.MapDisplay,
                            settings = Settings.HideMaps,
                        ),
                        MoreItem.Toggle(
                            label = SharedString.StationAccessibilityInfo,
                            settings = Settings.StationAccessibility,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.FeatureFlags,
                label = SharedString.FeatureFlagsSection,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = SharedString.DebugMode,
                            settings = Settings.DevDebugMode,
                        ),
                        MoreItem.Toggle(
                            label = SharedString.RouteSearch,
                            settings = Settings.SearchRouteResults,
                        ),
                        MoreItem.Toggle(
                            label = SharedString.Notifications,
                            settings = Settings.Notifications,
                        ),
                        MoreItem.Toggle(
                            label = SharedString.TrackThisTrip,
                            settings = Settings.TrackThisTrip,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Other,
                items =
                    listOf(
                        MoreItem.Link(
                            label = SharedString.TermsOfUse,
                            url = "https://www.mbta.com/policies/terms-use",
                        ),
                        MoreItem.Link(
                            label = SharedString.PrivacyPolicy,
                            url = "https://www.mbta.com/policies/privacy-policy",
                        ),
                        MoreItem.Link(
                            label = SharedString.ViewSourceOnGithub,
                            url = "https://github.com/mbta/mobile_app",
                        ),
                        MoreItem.NavLink(
                            label = SharedString.SoftwareLicenses,
                            callback = licensesCallback,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Support,
                label = SharedString.SupportSection,
                items = listOf(MoreItem.Phone("617-222-3200", "6172223200")),
                noteAbove = SharedString.SupportHours,
                noteBelow = SharedString.SupportAccessibilityNote,
            ),
        )
    }
}
