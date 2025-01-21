package com.mbta.tid.mbta_app.android.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.mbta.tid.mbta_app.analytics.Analytics

class AnalyticsProvider(private val firebaseAnalytics: FirebaseAnalytics) : Analytics() {
    override fun logEvent(name: String, parameters: Map<String, String>) {
        firebaseAnalytics.logEvent(name) {
            for ((key, value) in parameters) {
                param(key, value)
            }
        }
    }

    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
