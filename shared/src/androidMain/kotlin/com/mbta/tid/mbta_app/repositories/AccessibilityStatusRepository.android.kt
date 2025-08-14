package com.mbta.tid.mbta_app.repositories

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager

public class AccessibilityStatusRepository(context: Context) : IAccessibilityStatusRepository {
    private val accessibility =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?

    // https://stackoverflow.com/a/59950182
    override fun isScreenReaderEnabled(): Boolean {
        if (accessibility == null) return false
        if (!accessibility.isEnabled) return false
        val services =
            accessibility
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .filter {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.isAccessibilityTool
                    } else {
                        it.feedbackType != AccessibilityServiceInfo.FEEDBACK_GENERIC
                    }
                }
        return services.isNotEmpty()
    }
}
