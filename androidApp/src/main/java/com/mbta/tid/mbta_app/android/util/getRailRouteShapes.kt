package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import org.koin.compose.koinInject

@Composable
fun getRailRouteShapes(
    railRouteShapeRepository: IRailRouteShapeRepository = koinInject()
): MapFriendlyRouteResponse? {
    var railRouteShapes: MapFriendlyRouteResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(null) { railRouteShapes = railRouteShapeRepository.getRailRouteShapes() }

    return railRouteShapes
}
