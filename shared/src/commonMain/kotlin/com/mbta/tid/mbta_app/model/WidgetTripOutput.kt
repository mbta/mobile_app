package com.mbta.tid.mbta_app.model

/**
 * Wrapper for widget trip use case result. Allows representing "no trips found" (trip = null)
 * within ApiResult.Ok, since ApiResult requires non-null type parameter.
 */
public data class WidgetTripOutput(val trip: WidgetTripData?)
