package com.mbta.tid.mbta_app.map

object StopSourceGenerator {
    val stopSourceId = "stop-source"

    val propIdKey = "id"
    val propIsSelectedKey = "isSelected"
    val propIsTerminalKey = "isTerminal"
    // Map routes is an array of MapStopRoute enum names
    val propMapRoutesKey = "mapRoutes"
    val propNameKey = "name"
    // Route IDs are in a map keyed by MapStopRoute enum names, each with a list of IDs
    val propRouteIdsKey = "routeIds"
    val propServiceStatusKey = "serviceStatus"
    val propSortOrderKey = "sortOrder"
}
