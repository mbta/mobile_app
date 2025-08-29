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
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import io.github.dellisd.spatialk.geojson.Position
import kotlin.jvm.JvmName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IFavoritesViewModel {
    public val models: StateFlow<FavoritesViewModel.State>

    public fun reloadFavorites()

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setContext(context: FavoritesViewModel.Context)

    public fun setLocation(location: Position?)

    public fun setNow(now: EasternTimeInstant)

    public fun setIsFirstExposureToNewFavorites(isFirst: Boolean)

    public fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    )
}

public class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases,
    private val pinnedRoutesRepository: IPinnedRoutesRepository,
    private val tabPreferencesRepository: ITabPreferencesRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
) : MoleculeViewModel<FavoritesViewModel.Event, FavoritesViewModel.State>(), IFavoritesViewModel {

    public sealed class Context {
        public data object Favorites : Context()

        public data object Edit : Context()
    }

    public sealed interface Event {
        public data object ReloadFavorites : Event

        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event

        public data class UpdateFavorites(
            val updatedFavorites: Map<RouteStopDirection, Boolean>,
            val context: EditFavoritesContext,
            val defaultDirection: Int,
        ) : Event
    }

    public data class State(
        val awaitingPredictionsAfterBackground: Boolean,
        val favorites: Set<RouteStopDirection>?,
        val shouldShowFirstTimeToast: Boolean = false,
        val routeCardData: List<RouteCardData>?,
        val staticRouteCardData: List<RouteCardData>?,
        val loadedLocation: Position?,
    ) {
        public constructor() : this(false, null, false, null, null, null)
    }

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setContextState") private var context by mutableStateOf<Context?>(null)
    @set:JvmName("setIsFirstExposureToNewFavoritesState")
    private var isFirstExposureToNewFavorites by mutableStateOf(false)
    @set:JvmName("setLocationState") private var location by mutableStateOf<Position?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var favorites: Set<RouteStopDirection>? by remember { mutableStateOf(null) }

        var hadOldPinnedRoutes: Boolean by remember { mutableStateOf(false) }
        var shouldShowFirstTimeToast: Boolean by remember { mutableStateOf(false) }

        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var staticRouteCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var loadedLocation: Position? by remember { mutableStateOf(null) }

        var active: Boolean by remember { mutableStateOf(true) }

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
            hadOldPinnedRoutes = pinnedRoutesRepository.getPinnedRoutes().isNotEmpty()
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
                        RouteCardData.Context.Favorites,
                        favorites,
                        coroutineDispatcher,
                    )
                loadedLocation = location
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

        LaunchedEffect(hadOldPinnedRoutes, isFirstExposureToNewFavorites) {
            shouldShowFirstTimeToast = hadOldPinnedRoutes && isFirstExposureToNewFavorites
        }

        return State(
            awaitingPredictionsAfterBackground,
            favorites,
            shouldShowFirstTimeToast,
            routeCardData,
            staticRouteCardData,
            loadedLocation,
        )
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun reloadFavorites(): Unit = fireEvent(Event.ReloadFavorites)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setContext(context: Context) {
        this.context = context
    }

    override fun setLocation(location: Position?) {
        this.location = location
    }

    override fun setNow(now: EasternTimeInstant) {
        this.now = now
    }

    override fun setIsFirstExposureToNewFavorites(isFirst: Boolean) {
        this.isFirstExposureToNewFavorites = isFirst
    }

    override fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        fireEvent(Event.UpdateFavorites(updatedFavorites, context, defaultDirection))
    }
}

public class MockFavoritesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: FavoritesViewModel.State = FavoritesViewModel.State()) :
    IFavoritesViewModel {
    public var onReloadFavorites: () -> Unit = {}
    public var onSetActive: (Boolean, Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    private var onSetContext = { _: FavoritesViewModel.Context -> }
    public var onSetLocation: (Position?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant) -> Unit = { _ -> }
    public var onSetIsFirstExposureToNewFavorites: (Boolean) -> Unit = { _ -> }
    public var onUpdateFavorites: (Map<RouteStopDirection, Boolean>) -> Unit = { _ -> }

    override val models: MutableStateFlow<FavoritesViewModel.State> = MutableStateFlow(initialState)

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

    override fun setIsFirstExposureToNewFavorites(isFirstExposure: Boolean) {
        onSetIsFirstExposureToNewFavorites(isFirstExposure)
    }

    override fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        onUpdateFavorites(updatedFavorites)
    }
}
