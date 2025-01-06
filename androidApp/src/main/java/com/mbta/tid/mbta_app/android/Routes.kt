package com.mbta.tid.mbta_app.android

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable object NearbyTransit : Routes()

    @Serializable object More : Routes()
}
