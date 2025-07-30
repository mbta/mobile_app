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
import com.mbta.tid.mbta_app.repositories.DefaultTab
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IFavoritesViewModel {
    val models: StateFlow<FavoritesViewModel.State>

    fun reloadFavorites()

    fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    fun setAlerts(alerts: AlertsStreamDataResponse?)

    fun setContext(context: FavoritesViewModel.Context)

    fun setLocation(location: Position?)

    fun setNow(now: EasternTimeInstant)

    fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    )
}

class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases,
    private val tabPreferencesRepository: ITabPreferencesRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
) : MoleculeViewModel<FavoritesViewModel.Event, FavoritesViewModel.State>(), IFavoritesViewModel {

    sealed class Context {
        data object Favorites : Context()

        data object Edit : Context()
    }

    sealed interface Event {
        data object ReloadFavorites : Event

        data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event

        data class SetAlerts(val alerts: AlertsStreamDataResponse?) : Event

        data class SetContext(val context: Context) : Event

        data class SetLocation(val location: Position?) : Event

        data class SetNow(val now: EasternTimeInstant) : Event

        data class UpdateFavorites(
            val updatedFavorites: Map<RouteStopDirection, Boolean>,
            val context: EditFavoritesContext,
            val defaultDirection: Int,
        ) : Event
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
        var context: Context? by remember { mutableStateOf(null) }
        var location: Position? by remember { mutableStateOf(null) }
        var now: EasternTimeInstant by remember { mutableStateOf(EasternTimeInstant.now()) }

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
            // first time seeing favorites, default to nearby going forward
            tabPreferencesRepository.setDefaultTab(DefaultTab.Nearby)
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
                    is Event.SetContext -> context = event.context
                    is Event.SetLocation -> location = event.location
                    is Event.SetNow -> now = event.now
                    is Event.UpdateFavorites -> {
                        favoritesUsecases.updateRouteStopDirections(
                            event.updatedFavorites,
                            event.context,
                            event.defaultDirection,
                        )
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
                routeCardData =
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
                        favorites,
                        coroutineDispatcher,
                    )
            }
        }

        LaunchedEffect(stopIds, globalData, favorites, location) {
            if (stopIds == null || globalData == null) {
                staticRouteCardData = null
            } else if (stopIds.isEmpty()) {
                staticRouteCardData = emptyList()
            } else {
                staticRouteCardData =
                    RouteCardData.routeCardsForStaticStopList(
                        stopIds,
                        globalData,
                        RouteCardData.Context.Favorites,
                        // not depending on now because it only matters for testing
                        now,
                        location,
                        favorites,
                        coroutineDispatcher,
                    )
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

    override fun setContext(context: Context) = fireEvent(Event.SetContext(context))

    override fun setLocation(location: Position?) = fireEvent(Event.SetLocation(location))

    override fun setNow(now: EasternTimeInstant) = fireEvent(Event.SetNow(now))

    override fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        fireEvent(Event.UpdateFavorites(updatedFavorites, context, defaultDirection))
    }
}

class MockFavoritesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: FavoritesViewModel.State = FavoritesViewModel.State()) :
    IFavoritesViewModel {
    var onReloadFavorites = {}
    var onSetActive = { _: Boolean, _: Boolean -> }
    var onSetAlerts = { _: AlertsStreamDataResponse? -> }
    var onSetContext = { _: FavoritesViewModel.Context -> }
    var onSetLocation = { _: Position? -> }
    var onSetNow = { _: EasternTimeInstant -> }
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

    override fun setContext(context: FavoritesViewModel.Context) {
        onSetContext(context)
    }

    override fun setLocation(location: Position?) {
        onSetLocation(location)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }

    override fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        onUpdateFavorites(updatedFavorites)
    }
}
