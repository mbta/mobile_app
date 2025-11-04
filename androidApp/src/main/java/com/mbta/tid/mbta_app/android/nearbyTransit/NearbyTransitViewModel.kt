package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.maplibre.spatialk.geojson.Position

class NearbyTransitViewModel(private val nearbyRepository: INearbyRepository) :
    KoinComponent, ViewModel() {
    var loading by mutableStateOf(false)
    var nearbyStopIds by mutableStateOf<List<String>?>(null)
    var routeCardData by mutableStateOf<List<RouteCardData>?>(null)

    private var fetchNearbyTask: Job? = null

    fun getNearby(
        globalResponse: GlobalResponse,
        location: Position,
        setLastLocation: (Position) -> Unit,
        setIsTargeting: (Boolean) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (loading) {
                fetchNearbyTask?.cancel()
            }
            val stopIds = nearbyRepository.getStopIdsNearby(globalResponse, location)
            nearbyStopIds = stopIds
            setLastLocation(location)
            setIsTargeting(false)
        }
    }

    fun loadRouteCardData(
        global: GlobalResponse?,
        location: Position?,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        now: EasternTimeInstant,
    ) {
        val stopIds = nearbyStopIds
        if (global == null || location == null || stopIds == null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            routeCardData =
                RouteCardData.routeCardsForStopList(
                    stopIds,
                    global,
                    location,
                    schedules,
                    predictions,
                    alerts,
                    now,
                    RouteCardData.Context.NearbyTransit,
                )
        }
    }

    fun reset() {
        loading = false
        nearbyStopIds = null
    }

    override fun onCleared() {
        super.onCleared()
        fetchNearbyTask?.cancel()
    }
}
