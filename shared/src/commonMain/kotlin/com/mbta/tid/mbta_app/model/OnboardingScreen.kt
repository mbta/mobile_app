package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository

public enum class OnboardingScreen {
    Location,
    StationAccessibility,
    HideMaps,
    Feedback;

    internal fun applies(accessibilityStatus: IAccessibilityStatusRepository): Boolean =
        when (this) {
            Location -> true
            StationAccessibility -> true
            HideMaps -> accessibilityStatus.isScreenReaderEnabled()
            Feedback -> true
        }
}
