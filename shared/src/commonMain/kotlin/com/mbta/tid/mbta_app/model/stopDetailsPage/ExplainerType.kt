package com.mbta.tid.mbta_app.model.stopDetailsPage

import kotlinx.serialization.Serializable

@Serializable
public enum class ExplainerType {
    NoPrediction,
    FinishingAnotherTrip,
    NoVehicle,
}
