package com.mbta.tid.mbta_app.model.response

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class PredictionsByStopMessageResponse
internal constructor(
    @SerialName("stop_id") val stopId: String,
    internal val predictions: Map<String, Prediction>,
    internal val trips: Map<String, Trip>,
    internal val vehicles: Map<String, Vehicle>,
) {
    @DefaultArgumentInterop.Enabled
    internal constructor(
        objects: ObjectCollectionBuilder,
        stopId: String = objects.stops.keys.single(),
    ) : this(stopId, objects.predictions, objects.trips, objects.vehicles)

    override fun toString(): String = "[PredictionsByStopMessageResponse]"
}
