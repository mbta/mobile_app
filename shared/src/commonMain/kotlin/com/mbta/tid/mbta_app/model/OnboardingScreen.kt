package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository

enum class OnboardingScreen {
    Location,
    HideMaps,
    Feedback;

    fun applies(accessibilityStatus: IAccessibilityStatusRepository): Boolean =
        when (this) {
            Location -> true
            HideMaps -> accessibilityStatus.isScreenReaderEnabled()
            Feedback -> true
        }
}
