package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases = UsecaseDI().favoritesUsecases
) : ViewModel() {
    var favorites by mutableStateOf<Set<RouteStopDirection>?>(null)
    var routeCardData by mutableStateOf<List<RouteCardData>?>(null)

    suspend fun loadFavorites() {
        favorites = favoritesUsecases.getRouteStopDirectionFavorites()
    }

    fun loadRouteCardData(
        global: GlobalResponse?,
        location: Position?,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        now: Instant,
    ) {
        val stops = favorites?.map { global?.getStop(it.stop) }.orEmpty()
        val stopIds = stops.mapNotNull { it?.childStopIds?.plus(listOf(it.id)) }.flatten()
        if (stopIds.isEmpty()) {
            routeCardData = emptyList()
            return
        }
        if (global == null || location == null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val loadedRouteCardData =
                RouteCardData.routeCardsForStopList(
                    stopIds,
                    global,
                    location,
                    schedules,
                    predictions,
                    alerts,
                    now,
                    emptySet(),
                    RouteCardData.Context.Favorites,
                )
            routeCardData = filterRouteAndDirection(loadedRouteCardData, global)
        }
    }

    fun filterRouteAndDirection(
        routeCardData: List<RouteCardData>?,
        global: GlobalResponse,
    ): List<RouteCardData>? {
        return routeCardData?.filter { data ->
            data.stopData =
                data.stopData.filter { stopData ->
                    stopData.data =
                        stopData.data.filter { leafData ->
                            val routeStopDirection =
                                RouteStopDirection(
                                    leafData.lineOrRoute.id,
                                    leafData.stop.resolveParent(global).id,
                                    leafData.directionId,
                                )
                            favorites?.contains(routeStopDirection) == true
                        }
                    stopData.data.isNotEmpty()
                }
            data.stopData.any { it.data.isNotEmpty() }
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FavoritesViewModel() as T
        }
    }
}
