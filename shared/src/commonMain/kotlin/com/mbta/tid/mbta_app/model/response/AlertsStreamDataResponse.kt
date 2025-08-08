package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlinx.serialization.Serializable

@Serializable
public data class AlertsStreamDataResponse(internal val alerts: Map<String, Alert>) {
    public constructor(objects: ObjectCollectionBuilder) : this(objects.alerts)

    public fun getAlert(alertId: String): Alert? = alerts[alertId]

    public fun isEmpty(): Boolean = alerts.isEmpty()

    internal fun injectFacilities(globalResponse: GlobalResponse?): AlertsStreamDataResponse =
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

    override fun toString(): String = "[AlertsStreamDataResponse]"
}

public fun AlertsStreamDataResponse?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
