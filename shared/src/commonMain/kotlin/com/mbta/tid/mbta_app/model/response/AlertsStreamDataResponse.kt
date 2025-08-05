package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlinx.serialization.Serializable

@Serializable
data class AlertsStreamDataResponse(internal val alerts: Map<String, Alert>) {
    constructor(objects: ObjectCollectionBuilder) : this(objects.alerts)

    fun getAlert(alertId: String) = alerts[alertId]

    fun isEmpty() = alerts.isEmpty()

    fun injectFacilities(globalResponse: GlobalResponse?): AlertsStreamDataResponse =
        globalResponse?.let { global ->
            AlertsStreamDataResponse(
                alerts.mapValues { (_, alert) ->
                    val facilities =
                        alert.informedEntity
                            .mapNotNull { entity -> global.getFacility(entity.facility) }
                            .associateBy { it.id }
                    if (facilities.isEmpty()) alert else alert.copy(facilities = facilities)
                }
            )
        } ?: this

    override fun toString() = "[AlertsStreamDataResponse]"
}

fun AlertsStreamDataResponse?.isNullOrEmpty() = this == null || this.isEmpty()
