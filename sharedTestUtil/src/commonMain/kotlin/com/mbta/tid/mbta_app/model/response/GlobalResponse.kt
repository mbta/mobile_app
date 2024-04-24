package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

fun GlobalResponse.Companion.fromObjectCollection(
    objects: ObjectCollectionBuilder,
    patternIdsByStop: Map<String, List<String>>,
) =
    GlobalResponse(
        patternIdsByStop,
        objects.routes,
        objects.routePatterns,
        objects.stops,
        objects.trips
    )
