package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route

/**
 * A modal bottom sheet which may be open and should cover everything else.
 *
 * Named to rhyme with [Routes] and [SheetRoutes] but does not actually use the Android navigation
 * subsystem.
 */
sealed interface ModalRoutes {
    data class AlertDetails(
        val alertId: String,
        val line: Line?,
        val routes: List<Route>?,
        val stopId: String?
    ) : ModalRoutes
}
