package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.Settings

public enum class OnboardingScreen {
    Location,
    StationAccessibility,
    HideMaps,
    NotificationsBeta,
    Feedback;

    internal fun applies(
        accessibilityStatus: IAccessibilityStatusRepository,
        settings: Map<Settings, Boolean>,
    ): Boolean =
        when (this) {
            Location -> true
            StationAccessibility -> true
            HideMaps -> accessibilityStatus.isScreenReaderEnabled()
            NotificationsBeta -> settings[Settings.Notifications] == true
            Feedback -> true
        }
}
