package com.mbta.tid.mbta_app.model.stopDetailsPage

import kotlinx.serialization.Serializable

@Serializable
sealed class ExplainerType {
    data object NoPrediction : ExplainerType()

    data object FinishingAnotherTrip : ExplainerType()

    data object NoVehicle : ExplainerType()
}
