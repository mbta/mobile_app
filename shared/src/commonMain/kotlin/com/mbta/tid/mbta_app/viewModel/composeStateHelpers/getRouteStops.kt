package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
internal fun getRouteStops(
    routeId: String?,
    directionId: Int?,
    errorKey: String,
    routeStopsRepository: IRouteStopsRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    ioDispatcher: CoroutineDispatcher = koinInject(named("coroutineDispatcherIO")),
): RouteStopsResult? {
    var routeStops: RouteStopsResult? by remember { mutableStateOf(null) }

    fun fetchRouteStops() {
        if (routeId != null && directionId != null)
            CoroutineScope(ioDispatcher).launch {
                fetchApi(
                    errorBannerRepo = errorBannerRepository,
                    errorKey = errorKey,
                    getData = { routeStopsRepository.getRouteSegments(routeId, directionId) },
                    onSuccess = { routeStops = it },
                    onRefreshAfterError = { fetchRouteStops() },
                )
            }
    }

    LaunchedEffect(routeId, directionId) {
        routeStops = null
        fetchRouteStops()
    }

    return routeStops
}
