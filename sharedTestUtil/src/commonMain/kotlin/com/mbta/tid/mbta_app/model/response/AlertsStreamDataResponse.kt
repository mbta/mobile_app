package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

fun AlertsStreamDataResponse.Companion.fromObjectCollection(objects: ObjectCollectionBuilder) =
    AlertsStreamDataResponse(objects.alerts)
