package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Instant
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases = UsecaseDI().favoritesUsecases
) : ViewModel() {
    var favorites by mutableStateOf<Set<RouteStopDirection>?>(null)
    var routeCardData by mutableStateOf<List<RouteCardData>?>(null)

    suspend fun loadFavorites() {
        favorites = favoritesUsecases.getRouteStopDirectionFavorites()
    }

    suspend fun loadRealtimeRouteCardData(
        global: GlobalResponse?,
        location: Position?,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        now: Instant,
    ) {
        val stopIds = stopIds(global)
        if (stopIds.isEmpty()) {
            routeCardData = emptyList()
            return
        }
        if (global == null || location == null) {
            return
        }
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
        routeCardData = filterRouteAndDirection(loadedRouteCardData, global, favorites)
    }

    suspend fun loadStaticRouteCardData(
        global: GlobalResponse?,
        position: Position?,
    ): List<RouteCardData>? {
        val stopIds = stopIds(global)
        if (stopIds.isEmpty()) {
            return emptyList()
        }
        if (global == null) {
            return null
        }
        val loadedRouteCardData =
            RouteCardData.routeCardsForStaticStopList(
                stopIds,
                global,
                RouteCardData.Context.Favorites,
                sortByDistanceFrom = position,
            )
        return filterRouteAndDirection(loadedRouteCardData, global, favorites)
    }

    fun filterRouteAndDirection(
        routeCardData: List<RouteCardData>?,
        global: GlobalResponse,
        favorites: Set<RouteStopDirection>?,
    ): List<RouteCardData>? {
        return routeCardData
            ?.map { data ->
                val filteredStopData =
                    data.stopData
                        .map { stopData ->
                            val filteredLeafData =
                                stopData.data.filter { leafData ->
                                    val routeStopDirection =
                                        RouteStopDirection(
                                            leafData.lineOrRoute.id,
                                            leafData.stop.resolveParent(global).id,
                                            leafData.directionId,
                                        )
                                    favorites?.contains(routeStopDirection) == true
                                }
                            stopData.copy(data = filteredLeafData)
                        }
                        .filter { it.data.isNotEmpty() }
                data.copy(stopData = filteredStopData)
            }
            ?.filter { it.stopData.any { it.data.isNotEmpty() } }
    }

    fun updateFavorites(favorites: Map<RouteStopDirection, Boolean>?, onFinish: () -> Unit = {}) {
        if (favorites == null) return
        viewModelScope.launch {
            favoritesUsecases.updateRouteStopDirections(favorites)
            onFinish()
        }
    }

    private fun stopIds(global: GlobalResponse?): List<String> {
        val stops = favorites?.map { global?.getStop(it.stop) }.orEmpty()
        return stops.mapNotNull { it?.childStopIds?.plus(listOf(it.id)) }.flatten()
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FavoritesViewModel() as T
        }
    }
}
