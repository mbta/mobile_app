package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopCardData
import com.mbta.tid.mbta_app.model.filterValidFavorites
import com.mbta.tid.mbta_app.model.invalidFavorites
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.LoadedPredictions
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.LoadedSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.jvm.JvmName
import kotlin.native.ShouldRefineInSwift
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.sample
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalObjCRefinement::class)
public interface IFavoritesViewModel {
    public val models: StateFlow<FavoritesViewModel.State>

    public fun dismissNotificationsHint()

    public fun reloadFavorites()

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setContext(context: FavoritesViewModel.Context)

    public fun setLocation(location: Position?)

    public fun setNow(now: EasternTimeInstant)

    public fun setIsFirstExposureToNewFavorites(isFirst: Boolean)

    @ShouldRefineInSwift
    public fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int?,
        fcmToken: String?,
        locale: String?,
    )
}

public class FavoritesViewModel(
    private val favoritesUsecases: FavoritesUsecases,
    private val onboardingRepository: IOnboardingRepository,
    private val pinnedRoutesRepository: IPinnedRoutesRepository,
    private val sentryRepository: ISentryRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
) : MoleculeViewModel<FavoritesViewModel.Event, FavoritesViewModel.State>(), IFavoritesViewModel {

    public sealed class Context {
        public data object Favorites : Context()

        public data object Edit : Context()
    }

    public sealed interface Event {
        public data object DismissNotificationsHint : Event

        public data object ReloadFavorites : Event

        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event

        public data class UpdateFavorites(
            val updatedFavorites: Map<RouteStopDirection, FavoriteSettings?>,
            val context: EditFavoritesContext,
            val defaultDirection: Int?,
            val fcmToken: String?,
            val locale: String?,
        ) : Event
    }

    public data class State(
        val awaitingPredictionsAfterBackground: Boolean,
        val favorites: Map<RouteStopDirection, FavoriteSettings>?,
        val shouldShowFirstTimeToast: Boolean = false,
        val shouldShowNotificationsHint: Boolean = false,
        val routeCardData: List<RouteCardData>?,
        val stopCardData: List<StopCardData>?,
        val staticRouteCardData: List<RouteCardData>?,
        val staticStopCardData: List<StopCardData>?,
        val loadedLocation: Position?,
    ) {
        public constructor() : this(false, null, false, false, null, null, null, null, null)
    }

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setContextState") private var context by mutableStateOf<Context?>(null)
    @set:JvmName("setIsFirstExposureToNewFavoritesState")
    private var isFirstExposureToNewFavorites by mutableStateOf(false)
    @set:JvmName("setLocationState") private var location by mutableStateOf<Position?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @OptIn(FlowPreview::class)
    @Composable
    override fun runLogic(): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var favorites: Map<RouteStopDirection, FavoriteSettings>? by remember {
            mutableStateOf(null)
        }
        var validFavorites: Map<RouteStopDirection, FavoriteSettings>? by remember {
            mutableStateOf(null)
        }

        var hadOldPinnedRoutes: Boolean by remember { mutableStateOf(false) }
        var shouldShowFirstTimeToast: Boolean by remember { mutableStateOf(false) }
        var shouldShowNotificationsHint: Boolean by remember { mutableStateOf(false) }

        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var stopCardData: List<StopCardData>? by remember { mutableStateOf(null) }
        var staticRouteCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var staticStopCardData: List<StopCardData>? by remember { mutableStateOf(null) }
        var loadedLocation: Position? by remember { mutableStateOf(null) }

        var active: Boolean by remember { mutableStateOf(false) }
        val errorKey = ErrorKey(setOf(SheetRoutes.Favorites::class), "FavoritesViewModel")
        val globalData = getGlobalData(errorKey)

        val stopIds =
            remember(validFavorites, globalData) {
                val stops = validFavorites?.keys?.mapNotNull { globalData?.getStop(it.stop) }
                stops?.flatMap { stop ->
                    stop.childStopIds.filter { globalData?.stops?.containsKey(it) ?: false } +
                        stop.id
                }
            }
        val schedules = getSchedules(stopIds?.toSet(), errorKey)
        val predictions =
            subscribeToPredictions(
                stopIds?.toSet(),
                SheetRoutes.Favorites,
                active,
                errorKey,
                onAnyMessageReceived = { awaitingPredictionsAfterBackground = false },
            )

        LaunchedEffect(Unit) {
            val fetchedFavorites = favoritesUsecases.getRouteStopDirectionFavorites()
            hadOldPinnedRoutes = pinnedRoutesRepository.getPinnedRoutes().isNotEmpty()
            favorites = fetchedFavorites
            analytics.recordSession(fetchedFavorites.count())

            shouldShowNotificationsHint =
                onboardingRepository.notificationsFavoritesHintShouldShow()
        }

        LaunchedEffect(favorites, globalData) {
            val currentFavorites = favorites
            validFavorites = currentFavorites?.let {
                globalData?.let { global ->
                    currentFavorites.filterValidFavorites(global)
                } ?: it
            }

            // Log invalid favorites to Sentry so we can track how often they exist
            if (currentFavorites == null || globalData == null) return@LaunchedEffect
            val invalid = currentFavorites.keys.invalidFavorites(globalData)
            if (invalid.isEmpty()) return@LaunchedEffect
            sentryRepository.captureMessage(
                "FavoritesViewModel: ${invalid.size} favorite(s) are currently invalid and " +
                    "hidden from display"
            ) {
                addBreadcrumb(
                    Breadcrumb(
                        message = "Invalid favorites filtered out of display",
                        data =
                            invalid.entries.withIndex().associateTo(mutableMapOf()) { (index, entry)
                                ->
                                "favorite_$index" to
                                    mapOf(
                                        "route" to entry.key.route.idText,
                                        "stop" to entry.key.stop,
                                        "direction" to entry.key.direction,
                                        "reason" to entry.value,
                                    )
                            },
                    )
                )
            }
        }

        EventSink(eventHandlingTimeout = 2.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                Event.DismissNotificationsHint -> {
                    shouldShowNotificationsHint = false
                    onboardingRepository.notificationsFavoriteHintDismissed()
                }
                Event.ReloadFavorites ->
                    favorites = favoritesUsecases.getRouteStopDirectionFavorites()
                is Event.SetActive -> {
                    active = event.active
                    if (event.wasSentToBackground) {
                        awaitingPredictionsAfterBackground = true
                    }
                }
                is Event.UpdateFavorites -> {
                    for (favoriteSettings in event.updatedFavorites.values) {
                        if (favoriteSettings?.notifications?.enabled == true) {
                            favoriteSettings.notifications.windows.forEachIndexed { index, window ->
                                analytics.notificationsWindowSet(window, index)
                            }
                        }
                    }

                    favoritesUsecases.updateRouteStopDirections(
                        event.updatedFavorites,
                        event.context,
                        event.defaultDirection,
                        event.fcmToken,
                        event.locale,
                    )
                    reloadFavorites()
                }
            }
        }

        data class RouteCardDataParams(
            val location: Position?,
            val stopIds: List<String>?,
            val globalData: GlobalResponse?,
            val schedules: LoadedSchedules?,
            val predictions: LoadedPredictions?,
            val alerts: AlertsStreamDataResponse?,
            val now: EasternTimeInstant,
        )

        // Put all route card params into a single debounceable value. If we just put all the params
        // as keys to a LaunchedEffect, then routeCardData setting can get interrupted by frequent
        // changes to predictions or now, which can chain and significantly delay updates.
        var params: RouteCardDataParams? by remember { mutableStateOf(null) }
        LaunchedEffect(location, stopIds, globalData, schedules, predictions, alerts, now) {
            params =
                RouteCardDataParams(
                    location,
                    stopIds,
                    globalData,
                    schedules,
                    predictions,
                    alerts,
                    now,
                )
        }

        LaunchedEffect(Unit) {
            snapshotFlow { params }
                .sample(100.milliseconds)
                .conflate()
                .collect {
                    if (it == null) return@collect
                    if (it.stopIds == null || it.globalData == null) {
                        routeCardData = null
                    } else if (it.stopIds.isEmpty()) {
                        routeCardData = emptyList()
                    } else if (
                        it.location != null &&
                            it.schedules?.stopIds == it.stopIds.toSet() &&
                            it.predictions?.stopIds == it.stopIds.toSet()
                    ) {
                        routeCardData =
                            RouteCardData.routeCardsForStopList(
                                it.stopIds,
                                it.globalData,
                                it.location,
                                it.schedules.response,
                                it.predictions.response,
                                it.alerts,
                                it.now,
                                RouteCardData.Context.Favorites,
                                validFavorites?.keys,
                                coroutineDispatcher,
                            )
                        loadedLocation = it.location
                    }
                    stopCardData = routeCardData?.let { rcd ->
                        StopCardData.fromRouteCardData(rcd, sortByDistanceFrom = it.location)
                    }
                }
        }

        // Static data doesn't need to be processed in a snapshotFlow because the input params
        // change very infrequently
        LaunchedEffect(stopIds, globalData, validFavorites, location) {
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
                        validFavorites?.keys,
                        coroutineDispatcher,
                    )
            }
            staticStopCardData = staticRouteCardData?.let {
                StopCardData.fromRouteCardData(it, sortByDistanceFrom = location)
            }
        }

        LaunchedEffect(hadOldPinnedRoutes, isFirstExposureToNewFavorites) {
            shouldShowFirstTimeToast = hadOldPinnedRoutes && isFirstExposureToNewFavorites
        }

        return State(
            awaitingPredictionsAfterBackground,
            favorites,
            shouldShowFirstTimeToast,
            shouldShowNotificationsHint && favorites?.isNotEmpty() == true && stopCardData != null,
            routeCardData,
            stopCardData,
            staticRouteCardData,
            staticStopCardData,
            loadedLocation,
        )
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun dismissNotificationsHint(): Unit = fireEvent(Event.DismissNotificationsHint)

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
        updatedFavorites: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int?,
        fcmToken: String?,
        locale: String?,
    ) {
        fireEvent(
            Event.UpdateFavorites(updatedFavorites, context, defaultDirection, fcmToken, locale)
        )
    }
}

@OptIn(ExperimentalObjCRefinement::class)
public class MockFavoritesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: FavoritesViewModel.State = FavoritesViewModel.State()) :
    IFavoritesViewModel {
    public var onDismissNotificationsHint: () -> Unit = {}
    public var onReloadFavorites: () -> Unit = {}
    public var onSetActive: (Boolean, Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    private var onSetContext = { _: FavoritesViewModel.Context -> }
    public var onSetLocation: (Position?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant) -> Unit = { _ -> }
    public var onSetIsFirstExposureToNewFavorites: (Boolean) -> Unit = { _ -> }
    @ShouldRefineInSwift
    public var onUpdateFavorites: (Map<RouteStopDirection, FavoriteSettings?>) -> Unit = { _ -> }

    override val models: MutableStateFlow<FavoritesViewModel.State> = MutableStateFlow(initialState)

    override fun dismissNotificationsHint() {
        onDismissNotificationsHint()
    }

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

    override fun setIsFirstExposureToNewFavorites(isFirst: Boolean) {
        onSetIsFirstExposureToNewFavorites(isFirst)
    }

    override fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int?,
        fcmToken: String?,
        locale: String?,
    ) {
        onUpdateFavorites(updatedFavorites)
    }
}
