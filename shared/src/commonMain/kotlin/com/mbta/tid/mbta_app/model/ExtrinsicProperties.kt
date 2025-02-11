package com.mbta.tid.mbta_app.model

/** Grouped lines are displayed as though they are different branches of a single route. */
val Line.isGrouped
    get() = this.id in setOf("line-Green")

private val crCoreStations = setOf("place-north", "place-sstat", "place-bbsta", "place-rugg")

/**
 * Commuter Rail core stations have realtime track numbers displayed and track change alerts hidden.
 */
val Stop.isCRCore
    get() = this.id in crCoreStations || this.parentStationId in crCoreStations
