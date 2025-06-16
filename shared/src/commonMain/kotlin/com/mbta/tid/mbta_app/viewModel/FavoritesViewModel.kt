package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FavoritesViewModel(private val favoritesUsecases: FavoritesUsecases) :
    MoleculeViewModel<FavoritesViewModel.Event, FavoritesViewModel.State>() {
    sealed interface Event {
        data object ReloadFavorites : Event

        data class SetActive(val active: Boolean, val isReturningFromBackground: Boolean?) : Event

        data class SetAlerts(val alerts: AlertsStreamDataResponse?) : Event

        data class SetLocation(val location: Position?) : Event

        data class SetNow(val now: Instant) : Event
    }

    data class State(
        val favorites: Set<RouteStopDirection>?,
        val isReturningFromBackground: Boolean,
        val routeCardData: List<RouteCardData>?,
        val staticRouteCardData: List<RouteCardData>?,
    ) {
        constructor() : this(null, false, null, null)
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var favorites: Set<RouteStopDirection>? by remember { mutableStateOf(null) }
        var isReturningFromBackground: Boolean by remember { mutableStateOf(false) }
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var staticRouteCardData: List<RouteCardData>? by remember { mutableStateOf(null) }

        var active: Boolean by remember { mutableStateOf(true) }
        var alerts: AlertsStreamDataResponse? by remember { mutableStateOf(null) }
        var location: Position? by remember { mutableStateOf(null) }
        var now: Instant by remember { mutableStateOf(Clock.System.now()) }

        val globalData = getGlobalData("FavoritesViewModel.getGlobalData")
        val stopIds =
            remember(favorites, globalData) {
                val stops = favorites?.mapNotNull { globalData?.getStop(it.stop) }
                stops?.flatMap { stop ->
                    stop.childStopIds.filter { globalData?.stops?.containsKey(it) ?: false } +
                        stop.id
                }
            }
        val schedules = getSchedules(stopIds, "FavoritesViewModel.getSchedules")
        val predictions = subscribeToPredictions(stopIds, active)

        LaunchedEffect(Unit) { favorites = favoritesUsecases.getRouteStopDirectionFavorites() }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    Event.ReloadFavorites ->
                        favorites = favoritesUsecases.getRouteStopDirectionFavorites()
                    is Event.SetActive -> {
                        active = event.active
                        event.isReturningFromBackground?.let { isReturningFromBackground = it }
                    }
                    is Event.SetAlerts -> alerts = event.alerts
                    is Event.SetLocation -> location = event.location
                    is Event.SetNow -> now = event.now
                }
            }
        }

        LaunchedEffect(predictions) {
            if (predictions != null) {
                isReturningFromBackground = false
            }
        }

        LaunchedEffect(
            stopIds,
            globalData,
            location,
            schedules,
            predictions,
            alerts,
            now,
            favorites,
        ) {
            if (stopIds == null || globalData == null || location == null) {
                routeCardData = null
            } else if (stopIds.isEmpty()) {
                routeCardData = emptyList()
            } else {
                val loadedRouteCardData =
                    RouteCardData.routeCardsForStopList(
                        stopIds,
                        globalData,
                        location,
                        schedules,
                        predictions,
                        alerts,
                        now,
                        emptySet(),
                        RouteCardData.Context.Favorites,
                    )
                routeCardData = filterRouteAndDirection(loadedRouteCardData, globalData, favorites)
            }
        }

        LaunchedEffect(stopIds, globalData, favorites) {
            if (stopIds == null || globalData == null) {
                staticRouteCardData = null
            } else if (stopIds.isEmpty()) {
                staticRouteCardData = emptyList()
            } else {
                val loadedRouteCardData =
                    RouteCardData.routeCardsForStaticStopList(
                        stopIds,
                        globalData,
                        RouteCardData.Context.Favorites,
                    )
                staticRouteCardData =
                    filterRouteAndDirection(loadedRouteCardData, globalData, favorites)
            }
        }

        return State(favorites, isReturningFromBackground, routeCardData, staticRouteCardData)
    }

    val models
        get() = internalModels

    fun reloadFavorites() = fireEvent(Event.ReloadFavorites)

    @DefaultArgumentInterop.Enabled
    fun setActive(active: Boolean, isReturningFromBackground: Boolean? = null) =
        fireEvent(Event.SetActive(active, isReturningFromBackground))

    fun setAlerts(alerts: AlertsStreamDataResponse?) = fireEvent(Event.SetAlerts(alerts))

    fun setLocation(location: Position?) = fireEvent(Event.SetLocation(location))

    fun setNow(now: Instant) = fireEvent(Event.SetNow(now))

    companion object {
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
    }
}
