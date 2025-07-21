package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IFavoritesViewModel {
    val models: StateFlow<FavoritesViewModel.State>

    fun reloadFavorites()

    fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    fun setAlerts(alerts: AlertsStreamDataResponse?)

    fun setLocation(location: Position?)

    fun setNow(now: Instant)

    fun updateFavorites(updatedFavorites: Map<RouteStopDirection, Boolean>)
}

class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
) : MoleculeViewModel<FavoritesViewModel.Event, FavoritesViewModel.State>(), IFavoritesViewModel {

    sealed interface Event {
        data object ReloadFavorites : Event

        data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event

        data class SetAlerts(val alerts: AlertsStreamDataResponse?) : Event

        data class SetLocation(val location: Position?) : Event

        data class SetNow(val now: Instant) : Event

        data class UpdateFavorites(val updatedFavorites: Map<RouteStopDirection, Boolean>) : Event
    }

    data class State(
        val awaitingPredictionsAfterBackground: Boolean,
        val favorites: Set<RouteStopDirection>?,
        val routeCardData: List<RouteCardData>?,
        val staticRouteCardData: List<RouteCardData>?,
    ) {
        constructor() : this(false, null, null, null)
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var favorites: Set<RouteStopDirection>? by remember { mutableStateOf(null) }
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
        val predictions =
            subscribeToPredictions(
                stopIds,
                active,
                onAnyMessageReceived = { awaitingPredictionsAfterBackground = false },
            )

        LaunchedEffect(Unit) {
            val fetchedFavorites = favoritesUsecases.getRouteStopDirectionFavorites()
            favorites = fetchedFavorites
            analytics.recordSession(fetchedFavorites.count())
        }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    Event.ReloadFavorites ->
                        favorites = favoritesUsecases.getRouteStopDirectionFavorites()
                    is Event.SetActive -> {
                        active = event.active
                        if (event.wasSentToBackground) {
                            awaitingPredictionsAfterBackground = true
                        }
                    }
                    is Event.SetAlerts -> alerts = event.alerts
                    is Event.SetLocation -> location = event.location
                    is Event.SetNow -> now = event.now
                    is Event.UpdateFavorites -> {
                        favoritesUsecases.updateRouteStopDirections(event.updatedFavorites)
                        reloadFavorites()
                    }
                }
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
                        coroutineDispatcher,
                    )
                routeCardData = filterRouteAndDirection(loadedRouteCardData, globalData, favorites)
            }
        }

        LaunchedEffect(stopIds, globalData, favorites, location) {
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
                        // not depending on now because it only matters for testing
                        now,
                        location,
                        coroutineDispatcher,
                    )
                staticRouteCardData =
                    filterRouteAndDirection(loadedRouteCardData, globalData, favorites)
            }
        }

        return State(
            awaitingPredictionsAfterBackground,
            favorites,
            routeCardData,
            staticRouteCardData,
        )
    }

    override val models
        get() = internalModels

    override fun reloadFavorites() = fireEvent(Event.ReloadFavorites)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean) =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?) = fireEvent(Event.SetAlerts(alerts))

    override fun setLocation(location: Position?) = fireEvent(Event.SetLocation(location))

    override fun setNow(now: Instant) = fireEvent(Event.SetNow(now))

    override fun updateFavorites(updatedFavorites: Map<RouteStopDirection, Boolean>) {
        fireEvent(Event.UpdateFavorites(updatedFavorites))
    }

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

class MockFavoritesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: FavoritesViewModel.State = FavoritesViewModel.State()) :
    IFavoritesViewModel {
    var onReloadFavorites = {}
    var onSetActive = { _: Boolean, _: Boolean -> }
    var onSetAlerts = { _: AlertsStreamDataResponse? -> }
    var onSetLocation = { _: Position? -> }
    var onSetNow = { _: Instant -> }
    var onUpdateFavorites = { _: Map<RouteStopDirection, Boolean> -> }

    override val models = MutableStateFlow(initialState)

    override fun reloadFavorites() {
        onReloadFavorites()
    }

    override fun setActive(active: Boolean, wasSentToBackground: Boolean) {
        onSetActive(active, wasSentToBackground)
    }

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        onSetAlerts(alerts)
    }

    override fun setLocation(location: Position?) {
        onSetLocation(location)
    }

    override fun setNow(now: Instant) {
        onSetNow(now)
    }

    override fun updateFavorites(updatedFavorites: Map<RouteStopDirection, Boolean>) {
        onUpdateFavorites(updatedFavorites)
    }
}
