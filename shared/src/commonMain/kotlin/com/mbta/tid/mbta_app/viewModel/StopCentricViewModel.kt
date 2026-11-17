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
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.StopDetailsUtils
import com.mbta.tid.mbta_app.model.hasContext
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

    public val filterUpdates: StateFlow<StopDetailsPageFilters?>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setFilters(filters: StopDetailsPageFilters)

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
        public abstract val filteredWith: StopDetailsPageFilters

        public data class RouteStop(
            override val filteredWith: StopDetailsPageFilters,
            val stopData: RouteCardData.RouteStopData,
        ) : RouteData()

        public data class StopOverview(
            override val filteredWith: StopDetailsPageFilters,
            val routeCards: List<RouteCardData>,
        ) : RouteData()
    }

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setFiltersState")
    private var filters by mutableStateOf<StopDetailsPageFilters?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var routeData: RouteData? by remember { mutableStateOf(null) }
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }

        var active: Boolean by remember { mutableStateOf(true) }
        val errorKey = ErrorKey(setOf(SheetRoutes.StopDetails::class), "StopCentricViewModel")
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

        val departureData =
            getDepartureData(
                stopIds?.toSet(),
                now,
                filters?.let {
                    SheetRoutes.StopDetails(it.stopId, it.routeStopFilter, it.tripFilter)
                },
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
                    if (resolvedFilters.routeStopFilter != null) RouteCardData.Context.RouteStop
                    else RouteCardData.Context.StopOverview,
                )

            routeData =
                when {
                    routeCardData?.hasContext(RouteCardData.Context.RouteStop) == true ->
                        routeCardData
                            ?.find { it.lineOrRoute.id == resolvedFilters.routeStopFilter?.routeId }
                            ?.stopData
                            ?.firstOrNull()
                            ?.let { RouteData.RouteStop(resolvedFilters, it) }

                    routeCardData?.hasContext(RouteCardData.Context.StopOverview) == true ->
                        routeCardData?.let { RouteData.StopOverview(resolvedFilters, it) }

                    else -> null
                }
        }

        fun applyFilterUpdate(
            routeCardData: List<RouteCardData>?,
            currentFilters: StopDetailsPageFilters,
            routeStopFilterOverride: RouteStopFilter? = null,
        ) {
            if (routeCardData == null) return

            val autoTripFilter =
                StopDetailsUtils.autoTripFilter(
                    routeCardData,
                    routeStopFilterOverride ?: currentFilters.routeStopFilter,
                    currentFilters.tripFilter,
                    now,
                    globalData,
                )

            if (
                autoTripFilter != currentFilters.tripFilter ||
                    (routeStopFilterOverride != null &&
                        routeStopFilterOverride != currentFilters.routeStopFilter)
            ) {
                _filterUpdates.tryEmit(
                    StopDetailsPageFilters(
                        currentFilters.stopId,
                        routeStopFilterOverride ?: currentFilters.routeStopFilter,
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
                resolvedFilters.routeStopFilter
                    ?: StopDetailsUtils.autoRouteStopFilter(resolvedRouteCardData)
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

    private val _filterUpdates = MutableStateFlow<StopDetailsPageFilters?>(null)
    public override val filterUpdates: StateFlow<StopDetailsPageFilters?> = _filterUpdates

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setFilters(filters: StopDetailsPageFilters): Unit {
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
    public var onSetFilters: (filters: StopDetailsPageFilters) -> Unit = {}
    public var onSetNow: (now: EasternTimeInstant) -> Unit = {}

    override val models: MutableStateFlow<StopCentricViewModel.State> =
        MutableStateFlow(initialState)
    override val filterUpdates: MutableStateFlow<StopDetailsPageFilters?> = MutableStateFlow(null)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        onSetActive(active, wasSentToBackground)

    override fun setAlerts(alerts: AlertsStreamDataResponse?): Unit = onSetAlerts(alerts)

    override fun setFilters(filters: StopDetailsPageFilters): Unit = onSetFilters(filters)

    override fun setNow(now: EasternTimeInstant): Unit = onSetNow(now)
}
