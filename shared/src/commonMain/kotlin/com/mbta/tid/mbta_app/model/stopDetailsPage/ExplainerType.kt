package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.TrackChange
import kotlinx.serialization.Serializable

@Serializable
public sealed class ExplainerType {
    public object NoPrediction : ExplainerType()

    public object FinishingAnotherTrip : ExplainerType()

    public object NoVehicle : ExplainerType()

    public class ScheduledTrackChange(public val data: TrackChange.Scheduled) : ExplainerType()
}
