package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

class NearbyTransitViewModel(
    private val nearbyRepository: INearbyRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val analytics: Analytics,
) : KoinComponent, ViewModel() {
    var loading by mutableStateOf(false)
    var nearby by mutableStateOf<NearbyStaticData?>(null)
    var nearbyStopIds by mutableStateOf<List<String>?>(null)
    var routeCardData by mutableStateOf<List<RouteCardData>?>(null)

    private var fetchNearbyTask: Job? = null

    fun getNearby(
        globalResponse: GlobalResponse,
        location: Position,
        groupByDirection: Boolean,
        setLastLocation: (Position) -> Unit,
        setSelectingLocation: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (loading) {
                fetchNearbyTask?.cancel()
            }
            if (groupByDirection) {
                val stopIds = nearbyRepository.getStopIdsNearby(globalResponse, location)
                nearbyStopIds = stopIds
                setLastLocation(location)
                setSelectingLocation(false)
            } else {
                fetchNearbyTask = launch {
                    analytics.refetchedNearbyTransit()
                    loading = true
                    fetchApi(
                        errorBannerRepo = errorBannerRepository,
                        errorKey = "NearbyViewModel.getNearby",
                        getData = { nearbyRepository.getNearby(globalResponse, location) },
                        onSuccess = {
                            nearby = it
                            nearbyStopIds = it.stopIds().toList()
                        },
                        onRefreshAfterError = {
                            getNearby(
                                globalResponse,
                                location,
                                groupByDirection,
                                setLastLocation,
                                setSelectingLocation
                            )
                        }
                    )
                    loading = false
                    setLastLocation(location)
                    setSelectingLocation(false)
                }
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
                    pinnedRoutes ?: emptySet(),
                    RouteCardData.Context.NearbyTransit
                )
        }
    }

    fun reset() {
        loading = false
        nearby = null
        nearbyStopIds = null
    }

    override fun onCleared() {
        super.onCleared()
        fetchNearbyTask?.cancel()
    }
}
