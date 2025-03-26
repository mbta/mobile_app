package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

class NearbyTransitViewModel(
    private val nearbyRepository: INearbyRepository,
    private val settingsRepository: ISettingsRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val analytics: Analytics,
) : KoinComponent, ViewModel() {
    private val _showElevatorAccessibility: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showElevatorAccessibility: StateFlow<Boolean> = _showElevatorAccessibility
    private val _groupByDirection: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val groupByDirection: StateFlow<Boolean> = _groupByDirection

    var loadedLocation by mutableStateOf<Position?>(null)
    var loading by mutableStateOf(false)
    var nearby by mutableStateOf<NearbyStaticData?>(null)
    var routeCardData by mutableStateOf<List<RouteCardData>?>(null)

    private var fetchNearbyTask: Job? = null

    fun loadSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = settingsRepository.getSettings()
            _showElevatorAccessibility.value = data[Settings.ElevatorAccessibility] ?: false
            _groupByDirection.value = data[Settings.GroupByDirection] ?: false
        }
    }

    fun getNearby(
        globalResponse: GlobalResponse,
        location: Position,
        setLastLocation: (Position) -> Unit,
        setSelectingLocation: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (loadedLocation?.isRoughlyEqualTo(location) == true) {
                return@launch
            }
            if (loading) {
                fetchNearbyTask?.cancel()
            }
            fetchNearbyTask = launch {
                analytics.refetchedNearbyTransit()
                loading = true
                fetchApi(
                    errorBannerRepo = errorBannerRepository,
                    errorKey = "NearbyViewModel.getNearby",
                    getData = { nearbyRepository.getNearby(globalResponse, location) },
                    onSuccess = { nearby = it },
                    onRefreshAfterError = {
                        getNearby(globalResponse, location, setLastLocation, setSelectingLocation)
                    }
                )
                loading = false
                setLastLocation(location)
                setSelectingLocation(false)
            }
        }
    }

    fun loadRouteCardData(
        global: GlobalResponse?,
        location: Position?,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        now: Instant,
        pinnedRoutes: Set<String>?,
    ) {
        if (global == null || location == null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            routeCardData =
                RouteCardData.routeCardsForStopList(
                    nearbyRepository.getStopIdsNearby(global, location),
                    global,
                    location,
                    schedules,
                    predictions,
                    alerts,
                    now,
                    pinnedRoutes ?: emptySet(),
                    RouteCardData.Context.NearbyTransit
                )
        }
    }

    fun reset() {
        loadedLocation = null
        loading = false
        nearby = null
    }

    override fun onCleared() {
        super.onCleared()
        fetchNearbyTask?.cancel()
    }
}
