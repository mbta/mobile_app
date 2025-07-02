package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class RouteStopsFetcher(
    private val routeStopsRepository: IRouteStopsRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
) {

    fun getRouteStops(
        routeId: String,
        directionId: Int,
        errorKey: String,
        onSuccess: (RouteStopsResult) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            fetchApi(
                errorBannerRepo = errorBannerRepository,
                errorKey = errorKey,
                getData = { routeStopsRepository.getRouteStops(routeId, directionId) },
                onSuccess = { onSuccess(it) },
                onRefreshAfterError = { getRouteStops(routeId, directionId, errorKey, onSuccess) },
            )
        }
    }
}

class RouteStopsViewModel(
    routeStopsRepository: IRouteStopsRepository,
    errorBannerRepository: IErrorBannerStateRepository,
) : ViewModel() {

    private val routeStopsFetcher = RouteStopsFetcher(routeStopsRepository, errorBannerRepository)
    private val _routeStops = MutableStateFlow<RouteStopsResult?>(null)
    val routeStops: StateFlow<RouteStopsResult?> = _routeStops

    fun getRouteStops(routeId: String, directionId: Int, errorKey: String) {
        _routeStops.value = null
        routeStopsFetcher.getRouteStops(routeId, directionId, errorKey) { _routeStops.value = it }
    }

    class Factory(
        private val routeStopsRepository: IRouteStopsRepository,
        private val errorBannerRepository: IErrorBannerStateRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RouteStopsViewModel(routeStopsRepository, errorBannerRepository) as T
        }
    }
}

@Composable
fun getRouteStops(
    routeId: String,
    directionId: Int,
    errorKey: String,
    routeStopsRepository: IRouteStopsRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
): RouteStopsResult? {
    val viewModel: RouteStopsViewModel =
        viewModel(
            factory = RouteStopsViewModel.Factory(routeStopsRepository, errorBannerRepository)
        )

    LaunchedEffect(routeId, directionId) { viewModel.getRouteStops(routeId, directionId, errorKey) }

    return viewModel.routeStops.collectAsState(initial = null).value
}
