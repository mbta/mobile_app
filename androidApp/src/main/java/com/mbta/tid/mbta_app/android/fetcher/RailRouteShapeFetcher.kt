package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse

@Composable
fun getRailRouteShapes(backend: Backend): MapFriendlyRouteResponse? =
    getBackendData(backend, effectKey = null, Backend::getMapFriendlyRailShapes)
