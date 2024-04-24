package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

fun NearbyResponse.Companion.fromObjectCollection(objects: ObjectCollectionBuilder) =
    NearbyResponse(objects.stops.keys.toList())
