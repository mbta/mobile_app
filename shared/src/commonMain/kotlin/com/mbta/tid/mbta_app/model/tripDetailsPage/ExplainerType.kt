package com.mbta.tid.mbta_app.model.tripDetailsPage

import com.mbta.tid.mbta_app.model.TrackChange
import kotlinx.serialization.Serializable

@Serializable
public sealed class ExplainerType {
    @Serializable public object NoPrediction : ExplainerType()

    @Serializable public object FinishingAnotherTrip : ExplainerType()

    @Serializable public object NoVehicle : ExplainerType()

    @Serializable
    public class ScheduledTrackChange(public val data: TrackChange.Scheduled) : ExplainerType()
}
