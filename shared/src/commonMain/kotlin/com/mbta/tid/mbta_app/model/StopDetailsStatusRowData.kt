package com.mbta.tid.mbta_app.model

data class StopDetailsStatusRowData(
    val route: Route,
    val headsign: String,
    val formatted: RealtimePatterns.Format
)
