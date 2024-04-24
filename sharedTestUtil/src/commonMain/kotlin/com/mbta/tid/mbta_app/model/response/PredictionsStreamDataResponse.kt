package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

fun PredictionsStreamDataResponse.Companion.fromObjectCollection(objects: ObjectCollectionBuilder) =
    PredictionsStreamDataResponse(objects.predictions, objects.trips, objects.vehicles)
