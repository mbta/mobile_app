package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.utils.PerformsPoorlyInSwift
import kotlinx.serialization.Serializable

@Serializable
data class AlertsStreamDataResponse(@PerformsPoorlyInSwift val alerts: Map<String, Alert>) {
    constructor(objects: ObjectCollectionBuilder) : this(objects.alerts)

    fun getAlert(alertId: String) = alerts[alertId]
}
