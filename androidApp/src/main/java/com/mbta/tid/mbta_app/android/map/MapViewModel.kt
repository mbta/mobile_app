package com.mbta.tid.mbta_app.android.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mbta.tid.mbta_app.android.location.IViewportProvider
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteSourceData
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IMapViewModel {
    var railRouteSourceData: Flow<List<RouteSourceData>?>
    var stopSourceData: Flow<FeatureCollection?>
    var globalResponse: Flow<GlobalResponse?>
    var railRouteShapes: Flow<MapFriendlyRouteResponse?>
    val selectedVehicle: StateFlow<Vehicle?>
    val globalMapData: Flow<GlobalMapData?>
    val selectedStop: StateFlow<Stop?>
    val stopFilter: StateFlow<StopDetailsFilter?>
    val showRecenterButton: StateFlow<Boolean>
    val showTripCenterButton: StateFlow<Boolean>

    suspend fun globalMapData(now: Instant): GlobalMapData?

    suspend fun refreshGlobalMapData(now: Instant)

    suspend fun refreshRouteLineData(globalMapData: GlobalMapData?)

    suspend fun refreshStopFeatures(globalMapData: GlobalMapData?)

    suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?)

    suspend fun setGlobalResponse(globalResponse: GlobalResponse?)

    fun setSelectedVehicle(selectedVehicle: Vehicle?)

    fun setSelectedStop(stop: Stop?)

    fun setStopFilter(stopFilter: StopDetailsFilter?)

    fun updateCenterButtonVisibility(
        currentLocation: Location?,
        locationDataManager: LocationDataManager,
        isSearchExpanded: Boolean,
        viewportProvider: IViewportProvider,
    )

    fun hideCenterButtons()
}

open class MapViewModel : ViewModel(), IMapViewModel, KoinComponent {
    private val _railRouteSourceData = MutableStateFlow<List<RouteSourceData>?>(null)
    override var railRouteSourceData: Flow<List<RouteSourceData>?> = _railRouteSourceData
    private val _stopSourceData = MutableStateFlow<FeatureCollection?>(null)
    override var stopSourceData: Flow<FeatureCollection?> = _stopSourceData
    private val _globalMapData = MutableStateFlow<GlobalMapData?>(null)
    override var globalMapData: Flow<GlobalMapData?> = _globalMapData
    private val _globalResponse = MutableStateFlow<GlobalResponse?>(null)
    override var globalResponse: Flow<GlobalResponse?> = _globalResponse
    private val _railRouteShapes = MutableStateFlow<MapFriendlyRouteResponse?>(null)
    override var railRouteShapes: Flow<MapFriendlyRouteResponse?> = _railRouteShapes
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    override val selectedVehicle = _selectedVehicle.asStateFlow()
    private val _selectedStop = MutableStateFlow<Stop?>(null)
    override val selectedStop = _selectedStop.asStateFlow()
    private val _stopFilter = MutableStateFlow<StopDetailsFilter?>(null)
    override val stopFilter = _stopFilter.asStateFlow()
    private val _showRecenterButton = MutableStateFlow(false)
    override val showRecenterButton = _showRecenterButton.asStateFlow()
    private val _showTripCenterButton = MutableStateFlow(false)
    override val showTripCenterButton = _showTripCenterButton.asStateFlow()
    private val alertsData = MutableStateFlow<AlertsStreamDataResponse?>(null)
    private val railRouteShapeRepository: IRailRouteShapeRepository by inject()

    init {
        viewModelScope.launch { _railRouteShapes.value = fetchRailRouteShapes() }
        viewModelScope.launch { setUpSubscriptions() }
    }

    private suspend fun setUpSubscriptions() {
        merge(_globalResponse, alertsData).collectLatest {
            refreshGlobalMapData(Clock.System.now())
            refreshRouteLineData(_globalMapData.value)
            refreshStopFeatures(_globalMapData.value)
        }
    }

    override suspend fun globalMapData(now: Instant): GlobalMapData? =
        withContext(Dispatchers.Default) {
            globalResponse.first()?.let { GlobalMapData(it, alertsData.value, now) }
        }

    override suspend fun refreshGlobalMapData(now: Instant) {
        _globalMapData.value = globalMapData(now)
    }

    override suspend fun refreshRouteLineData(globalMapData: GlobalMapData?) {
        val globalResponse = globalResponse.first() ?: return
        val railRouteShapes = railRouteShapes.first() ?: return
        _railRouteSourceData.value =
            RouteFeaturesBuilder.generateRouteSources(
                railRouteShapes.routesWithSegmentedShapes,
                globalResponse,
                globalMapData,
            )
    }

    override suspend fun refreshStopFeatures(globalMapData: GlobalMapData?) {
        val routeLineData = railRouteSourceData.first() ?: return
        _stopSourceData.value = StopFeaturesBuilder.buildCollection(globalMapData, routeLineData)
    }

    override suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?) {
        this.alertsData.value = alertsData
    }

    override suspend fun setGlobalResponse(globalResponse: GlobalResponse?) {
        _globalResponse.value = globalResponse
    }

    override fun setSelectedVehicle(selectedVehicle: Vehicle?) {
        _selectedVehicle.value = selectedVehicle
    }

    override fun setSelectedStop(stop: Stop?) {
        _selectedStop.value = stop
    }

    override fun setStopFilter(stopFilter: StopDetailsFilter?) {
        _stopFilter.value = stopFilter
    }

    fun setShowRecenterButton(show: Boolean) {
        _showRecenterButton.value = show
    }

    fun setShowTripCenterButton(show: Boolean) {
        _showTripCenterButton.value = show
    }

    override fun updateCenterButtonVisibility(
        currentLocation: Location?,
        locationDataManager: LocationDataManager,
        isSearchExpanded: Boolean,
        viewportProvider: IViewportProvider,
    ) {
        setShowRecenterButton(
            locationDataManager.hasPermission &&
                currentLocation != null &&
                !viewportProvider.isFollowingPuck
        )
        setShowTripCenterButton(
            selectedVehicle.value != null &&
                !isSearchExpanded &&
                !viewportProvider.isVehicleOverview
        )
    }

    override fun hideCenterButtons() {
        setShowRecenterButton(false)
        setShowTripCenterButton(false)
    }

    private suspend fun fetchRailRouteShapes(): MapFriendlyRouteResponse? {
        return when (val data = railRouteShapeRepository.getRailRouteShapes()) {
            is ApiResult.Ok -> data.data
            is ApiResult.Error -> {
                Log.e("MapViewModel", "fetchRailRouteShapes failed: $data")
                null
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel() as T
        }
    }
}
