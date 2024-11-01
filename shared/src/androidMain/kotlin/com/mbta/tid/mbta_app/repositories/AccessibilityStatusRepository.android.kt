package com.mbta.tid.mbta_app.repositories

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

class AccessibilityStatusRepository(context: Context) : IAccessibilityStatusRepository {
    private val accessibility =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?

    // https://stackoverflow.com/a/59950182
    override fun isScreenReaderEnabled(): Boolean {
        if (accessibility == null) return false
        return accessibility.isEnabled &&
            accessibility
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .isNotEmpty()
    }
}
