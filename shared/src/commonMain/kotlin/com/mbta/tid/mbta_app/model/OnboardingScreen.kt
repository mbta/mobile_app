package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository

enum class OnboardingScreen {
    Location,
    StationAccessibility,
    MapDisplay,
    Feedback;

    fun applies(accessibilityStatus: IAccessibilityStatusRepository): Boolean =
        when (this) {
            Location -> true
            StationAccessibility -> true
            MapDisplay -> accessibilityStatus.isScreenReaderEnabled()
            Feedback -> true
        }
}
