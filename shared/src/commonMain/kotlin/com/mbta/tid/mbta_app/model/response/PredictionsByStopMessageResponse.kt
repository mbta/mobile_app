package com.mbta.tid.mbta_app.model.response

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.PerformsPoorlyInSwift
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionsByStopMessageResponse(
    @SerialName("stop_id") val stopId: String,
    @PerformsPoorlyInSwift val predictions: Map<String, Prediction>,
    @PerformsPoorlyInSwift val trips: Map<String, Trip>,
    @PerformsPoorlyInSwift val vehicles: Map<String, Vehicle>
) {
    @DefaultArgumentInterop.Enabled
    constructor(
        objects: ObjectCollectionBuilder,
        stopId: String = objects.stops.keys.single()
    ) : this(stopId, objects.predictions, objects.trips, objects.vehicles)
}
