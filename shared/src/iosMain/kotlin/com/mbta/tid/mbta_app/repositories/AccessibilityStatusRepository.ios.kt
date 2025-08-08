package com.mbta.tid.mbta_app.repositories

import org.koin.core.component.KoinComponent
import platform.UIKit.UIAccessibilityIsVoiceOverRunning

public class AccessibilityStatusRepository : IAccessibilityStatusRepository, KoinComponent {
    override fun isScreenReaderEnabled(): Boolean = UIAccessibilityIsVoiceOverRunning()
}
