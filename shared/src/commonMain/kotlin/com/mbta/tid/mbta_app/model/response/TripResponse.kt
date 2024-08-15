package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.Serializable

@Serializable data class TripResponse(val trip: Trip)
