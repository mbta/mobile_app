package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.RouteStopPageFilters
import com.mbta.tid.mbta_app.model.RouteStopUtils
import com.mbta.tid.mbta_app.model.StopCentricPageFilters
import com.mbta.tid.mbta_app.model.StopOverviewPageFilters
import com.mbta.tid.mbta_app.model.StopOverviewUtils
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getDepartureData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IStopCentricViewModel {
    public val models: StateFlow<StopCentricViewModel.State>

    public val filterUpdates: StateFlow<RouteStopPageFilters?>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setFilters(filters: StopCentricPageFilters)

    public fun setNow(now: EasternTimeInstant)
}

public class StopCentricViewModel(
    private val routeCardDataViewModel: IRouteCardDataViewModel,
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val predictionsRepository: IPredictionsRepository,
    private val sentryRepository: ISentryRepository,
    private val schedulesRepository: ISchedulesRepository,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) :
    MoleculeViewModel<StopCentricViewModel.Event, StopCentricViewModel.State>(),
    IStopCentricViewModel {
    public sealed class Event {
        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event()
    }

    public data class State(
        val routeData: RouteData? = null,
        val awaitingPredictionsAfterBackground: Boolean = false,
    )

    public sealed class RouteData {
        public abstract val filteredWith: StopCentricPageFilters

        public data class RouteStop(
            override val filteredWith: RouteStopPageFilters,
            val stopData: RouteCardData.RouteStopData,
        ) : RouteData()

        public data class StopOverview(
            override val filteredWith: StopOverviewPageFilters,
            val routeCards: List<RouteCardData>,
        ) : RouteData()
    }

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setFiltersState")
    private var filters by mutableStateOf<StopCentricPageFilters?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var routeData: RouteData? by remember { mutableStateOf(null) }
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }

        var active: Boolean by remember { mutableStateOf(true) }
        val errorKey =
            ErrorKey(
                setOf(SheetRoutes.StopOverview::class, SheetRoutes.RouteStop::class),
                "StopCentricViewModel",
            )
        val globalData = getGlobalData(errorKey, coroutineDispatcher = coroutineDispatcher)

        val stopIds =
            remember(filters, globalData) {
                filters?.stopId?.let { stopId ->
                    listOf(stopId) +
                        (globalData?.getStop(stopId)?.let { stop ->
                            stop.childStopIds.filter { globalData.stops.containsKey(it) }
                        } ?: emptyList())
                }
            }

        val sheetRoute =
            remember(filters) {
                when (val filters = filters) {
                    null -> null
                    is StopOverviewPageFilters -> SheetRoutes.StopOverview(filters.stopId)
                    is RouteStopPageFilters ->
                        SheetRoutes.RouteStop(
                            filters.stopId,
                            filters.routeStopFilter,
                            filters.tripFilter,
                        )
                }
            }

        val departureData =
            getDepartureData(
                stopIds?.toSet(),
                now,
                sheetRoute,
                active,
                errorKey,
                onAnyMessageReceived = { awaitingPredictionsAfterBackground = false },
                errorBannerRepository,
                schedulesRepository,
                predictionsRepository,
                coroutineDispatcher,
            )

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                is Event.SetActive -> {
                    active = event.active
                    if (event.wasSentToBackground) {
                        awaitingPredictionsAfterBackground = true
                    }
                }
            }
        }

        LaunchedEffect(stopIds, globalData, departureData, alerts, now, filters) {
            val resolvedFilters = filters
            val responses = stopIds?.let { departureData.matchingResponses(it.toSet(), now) }
            if (
                stopIds == null ||
                    globalData == null ||
                    responses == null ||
                    resolvedFilters == null
            ) {
                routeCardData = null
                routeData = null
                return@LaunchedEffect
            } else if (routeData?.filteredWith?.stopId != filters?.stopId) {
                routeCardData = null
                routeData = null
            }

            routeCardData =
                RouteCardData.routeCardsForStopList(
                    stopIds,
                    globalData,
                    sortByDistanceFrom = null,
                    responses.schedules,
                    responses.predictions,
                    alerts,
                    now,
                    if (resolvedFilters is RouteStopPageFilters) RouteCardData.Context.RouteStop
                    else RouteCardData.Context.StopOverview,
                )

            routeData =
                when (resolvedFilters) {
                    is RouteStopPageFilters ->
                        routeCardData
                            ?.find { it.lineOrRoute.id == resolvedFilters.routeStopFilter.routeId }
                            ?.stopData
                            ?.firstOrNull()
                            ?.let { RouteData.RouteStop(resolvedFilters, it) }

                    is StopOverviewPageFilters ->
                        routeCardData?.let { RouteData.StopOverview(resolvedFilters, it) }
                }
        }

        fun applyFilterUpdate(
            routeCardData: List<RouteCardData>?,
            currentFilters: StopCentricPageFilters,
            routeStopFilterOverride: RouteStopFilter? = null,
        ) {
            if (routeCardData == null) return

            val currentRouteStopFilters = currentFilters as? RouteStopPageFilters

            val autoTripFilter =
                RouteStopUtils.autoTripFilter(
                    routeCardData,
                    routeStopFilterOverride ?: currentRouteStopFilters?.routeStopFilter,
                    currentRouteStopFilters?.tripFilter,
                    now,
                    globalData,
                )

            val newRouteStopFilter =
                routeStopFilterOverride ?: currentRouteStopFilters?.routeStopFilter ?: return

            if (
                autoTripFilter != currentRouteStopFilters?.tripFilter ||
                    newRouteStopFilter != currentRouteStopFilters?.routeStopFilter
            ) {
                _filterUpdates.tryEmit(
                    RouteStopPageFilters(
                        currentFilters.stopId,
                        newRouteStopFilter,
                        autoTripFilter,
                    ),
                )
            }
        }

        LaunchedEffect(filters) { filters?.let { applyFilterUpdate(routeCardData, it) } }

        LaunchedEffect(routeCardData) {
            routeCardDataViewModel.setRouteCardData(routeCardData)

            val resolvedFilters = filters
            val resolvedRouteCardData = routeCardData
            if (resolvedRouteCardData == null || resolvedFilters == null) {
                return@LaunchedEffect
            }

            val autoRouteStopFilter =
                when (resolvedFilters) {
                    is RouteStopPageFilters -> resolvedFilters.routeStopFilter
                    is StopOverviewPageFilters ->
                        StopOverviewUtils.autoRouteStopFilter(resolvedRouteCardData)
                }
            applyFilterUpdate(resolvedRouteCardData, resolvedFilters, autoRouteStopFilter)
        }

        val state =
            remember(filters, routeData, awaitingPredictionsAfterBackground) {
                State(routeData, awaitingPredictionsAfterBackground)
            }

        return state
    }

    override val models: StateFlow<State>
        get() = internalModels

    private val _filterUpdates = MutableStateFlow<RouteStopPageFilters?>(null)
    public override val filterUpdates: StateFlow<RouteStopPageFilters?> = _filterUpdates

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setFilters(filters: StopCentricPageFilters): Unit {
        this.filters = filters
        // If the filter is set, the last pushed update should be cleared
        _filterUpdates.tryEmit(null)
    }

    override fun setNow(now: EasternTimeInstant): Unit {
        this.now = now
    }
}

public class MockStopCentricViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: StopCentricViewModel.State = StopCentricViewModel.State()) :
    IStopCentricViewModel {

    public var onSetActive: (active: Boolean, wasSentToBackground: Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (alerts: AlertsStreamDataResponse?) -> Unit = {}
    public var onSetFilters: (filters: StopCentricPageFilters) -> Unit = {}
    public var onSetNow: (now: EasternTimeInstant) -> Unit = {}

    override val models: MutableStateFlow<StopCentricViewModel.State> =
        MutableStateFlow(initialState)
    override val filterUpdates: MutableStateFlow<RouteStopPageFilters?> = MutableStateFlow(null)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        onSetActive(active, wasSentToBackground)

    override fun setAlerts(alerts: AlertsStreamDataResponse?): Unit = onSetAlerts(alerts)

    override fun setFilters(filters: StopCentricPageFilters): Unit = onSetFilters(filters)

    override fun setNow(now: EasternTimeInstant): Unit = onSetNow(now)
}
