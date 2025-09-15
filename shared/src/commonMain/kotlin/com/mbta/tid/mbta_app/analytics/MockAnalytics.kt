package com.mbta.tid.mbta_app.analytics

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop

public class MockAnalytics
@DefaultArgumentInterop.Enabled
constructor(
    internal val onLogEvent: (String, Map<String, String>) -> Unit = { _, _ -> },
    internal val onSetUserProperty: (String, String) -> Unit = { _, _ -> },
) : Analytics() {
    override fun logEvent(name: String, parameters: Map<String, String>) {
        onLogEvent(name, parameters)
    }

    override fun setUserProperty(name: String, value: String) {
        onSetUserProperty(name, value)
    }
}
