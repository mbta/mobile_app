package com.mbta.tid.mbta_app.model

internal data class StopDetailsStatusRowData(
    val route: Route,
    val headsign: String,
    val formatted: UpcomingFormat,
)
