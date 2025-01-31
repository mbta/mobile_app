package com.mbta.tid.mbta_app.model.stopDetailsPage

import kotlinx.serialization.Serializable

@Serializable
sealed class ExplainerType {
    @Serializable data object NoPrediction : ExplainerType()

    @Serializable data object FinishingAnotherTrip : ExplainerType()

    @Serializable data object NoVehicle : ExplainerType()
}
