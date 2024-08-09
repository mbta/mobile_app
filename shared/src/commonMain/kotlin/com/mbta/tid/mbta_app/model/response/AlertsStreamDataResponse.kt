package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlinx.serialization.Serializable

@Serializable
data class AlertsStreamDataResponse(val alerts: Map<String, Alert>) {
    constructor(objects: ObjectCollectionBuilder) : this(objects.alerts)

    // TODO remove after debugging mixed shuttle/suspension alerts
    fun splitAlert566172(): AlertsStreamDataResponse =
        when (val alert566172 = alerts["566172"]) {
            null -> this
            else -> {
                val shuttledStops =
                    setOf("place-welln", "70032", "70033") +
                        setOf("place-astao", "70278", "70279") +
                        setOf("place-sull", "70030", "70031") +
                        setOf("place-ccmnl", "70028", "70029") +
                        setOf("place-north", "70026", "70027")
                val suspendedStops =
                    setOf("place-north", "70026", "70027") +
                        setOf("place-haecl", "70024", "70025") +
                        setOf("place-state", "70022", "70023") +
                        setOf("place-dwnxg", "70020", "70021") +
                        setOf("place-chncl", "70018", "70019") +
                        setOf("place-tumnl", "70016", "70017") +
                        setOf("place-bbsta", "70014", "70015")
                AlertsStreamDataResponse(
                    alerts - listOf("566172") +
                        mapOf(
                            "566172.1" to
                                Alert(
                                    id = "566172.1",
                                    activePeriod = alert566172.activePeriod,
                                    cause = null,
                                    description = null,
                                    effect = Alert.Effect.Shuttle,
                                    effectName = null,
                                    informedEntity =
                                        alert566172.informedEntity.filter {
                                            it.stop in shuttledStops
                                        },
                                    header = null,
                                    lifecycle = alert566172.lifecycle,
                                    updatedAt = alert566172.updatedAt
                                ),
                            "566172.2" to
                                Alert(
                                    id = "566172.2",
                                    activePeriod = alert566172.activePeriod,
                                    cause = null,
                                    description = null,
                                    effect = Alert.Effect.Suspension,
                                    effectName = null,
                                    informedEntity =
                                        alert566172.informedEntity.filter {
                                            it.stop in suspendedStops
                                        },
                                    header = null,
                                    lifecycle = alert566172.lifecycle,
                                    updatedAt = alert566172.updatedAt
                                )
                        )
                )
            }
        }
}
