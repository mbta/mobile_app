package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import kotlinx.serialization.Serializable

@Serializable
data class AlertsStreamDataResponse(val alerts: Map<String, Alert>) : StreamDataResponse {
    override val countSummary = "${alerts.size} alerts"
}
