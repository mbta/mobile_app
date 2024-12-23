package com.mbta.tid.mbta_app.android.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.FeatureCollection
import com.mbta.tid.mbta_app.android.util.LaunchedOnDispatcherEffect
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteLineData
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopSourceData
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IMapViewModel {
    var lastMapboxErrorTimestamp: Flow<Instant?>
    var railRouteLineData: Flow<List<RouteLineData>?>
    var stopSourceData: Flow<FeatureCollection?>
    var globalResponse: Flow<GlobalResponse?>
    var railRouteShapes: Flow<MapFriendlyRouteResponse?>

    suspend fun loadConfig()

    suspend fun globalMapData(now: Instant): GlobalMapData?

    @Composable fun rememberGlobalMapData(now: Instant): GlobalMapData?

    suspend fun refreshRouteLineData(now: Instant)

    suspend fun refreshStopFeatures(now: Instant, selectedStop: Stop?)

    suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?)

    suspend fun setGlobalResponse(globalResponse: GlobalResponse?)
}

open class MapViewModel(
    private val configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
    val configureMapboxToken: (String) -> Unit = { token -> MapboxOptions.accessToken = token },
    setHttpInterceptor: (MapHttpInterceptor?) -> Unit = { interceptor ->
        HttpServiceFactory.setHttpServiceInterceptor(interceptor)
    }
) : ViewModel(), IMapViewModel, KoinComponent {
    private val _config = MutableStateFlow<ApiResult<ConfigResponse>?>(null)
    var config: StateFlow<ApiResult<ConfigResponse>?> = _config
    private val _lastMapboxErrorTimestamp = MutableStateFlow<Instant?>(null)
    override var lastMapboxErrorTimestamp = _lastMapboxErrorTimestamp.debounce(1.seconds)
    private val _railRouteLineData = MutableStateFlow<List<RouteLineData>?>(null)
    override var railRouteLineData: Flow<List<RouteLineData>?> = _railRouteLineData
    private val _stopSourceData = MutableStateFlow<FeatureCollection?>(null)
    override var stopSourceData: Flow<FeatureCollection?> = _stopSourceData
    private val _globalResponse = MutableStateFlow<GlobalResponse?>(null)
    override var globalResponse: Flow<GlobalResponse?> = _globalResponse
    private val _railRouteShapes = MutableStateFlow<MapFriendlyRouteResponse?>(null)
    override var railRouteShapes: Flow<MapFriendlyRouteResponse?> = _railRouteShapes

    private var alertsData: AlertsStreamDataResponse? by mutableStateOf(null)
    private val railRouteShapeRepository: IRailRouteShapeRepository by inject()

    init {
        setHttpInterceptor(MapHttpInterceptor { updateLastErrorTimestamp() })
        viewModelScope.launch { _railRouteShapes.value = fetchRailRouteShapes() }
    }

    private fun updateLastErrorTimestamp() {
        _lastMapboxErrorTimestamp.value = Clock.System.now()
    }

    override suspend fun loadConfig() {
        val latestConfig = configUseCase.getConfig()
        if (latestConfig is ApiResult.Ok) {
            configureMapboxToken(latestConfig.data.mapboxPublicToken)
        }
        _config.value = latestConfig
    }

    override suspend fun globalMapData(now: Instant): GlobalMapData? =
        globalResponse.first()?.let {
            GlobalMapData(it, GlobalMapData.getAlertsByStop(it, alertsData, now))
        }

    @Composable
    override fun rememberGlobalMapData(now: Instant): GlobalMapData? {
        var result by remember { mutableStateOf<GlobalMapData?>(null) }
        val globalResponse by this.globalResponse.collectAsState(initial = null)
        LaunchedOnDispatcherEffect(Dispatchers.IO, this.alertsData, globalResponse, now) {
            result = globalMapData(now)
        }
        return result
    }

    override suspend fun refreshRouteLineData(now: Instant) {
        val globalResponse = globalResponse.first() ?: return
        val railRouteShapes = railRouteShapes.first() ?: return
        _railRouteLineData.value =
            RouteFeaturesBuilder.generateRouteLines(
                railRouteShapes.routesWithSegmentedShapes,
                globalResponse.routes,
                globalResponse.stops,
                globalMapData(now)?.alertsByStop
            )
    }

    override suspend fun refreshStopFeatures(now: Instant, selectedStop: Stop?) {
        val routeLineData = railRouteLineData.first() ?: return
        _stopSourceData.value =
            StopFeaturesBuilder.buildCollection(
                    StopSourceData(selectedStopId = selectedStop?.id),
                    globalMapData(now)?.mapStops.orEmpty(),
                    routeLineData
                )
                .toMapbox()
    }

    override suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?) {
        this.alertsData = alertsData
    }

    override suspend fun setGlobalResponse(globalResponse: GlobalResponse?) {
        _globalResponse.value = globalResponse
    }

    private suspend fun fetchRailRouteShapes(): MapFriendlyRouteResponse? {
        return when (val data = railRouteShapeRepository.getRailRouteShapes()) {
            is ApiResult.Ok -> data.data
            is ApiResult.Error -> null
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel() as T
        }
    }
}
