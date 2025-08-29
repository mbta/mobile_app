package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.StopDetailsUtils
import com.mbta.tid.mbta_app.model.hasContext
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IStopDetailsViewModel {
    public val models: StateFlow<StopDetailsViewModel.State>

    public val filterUpdates: StateFlow<StopDetailsPageFilters?>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setAlertSummaries(alertSummaries: Map<String, AlertSummary?>)

    public fun setFilters(filters: StopDetailsPageFilters)

    public fun setNow(now: EasternTimeInstant)
}

public class StopDetailsViewModel(
    private val routeCardDataViewModel: IRouteCardDataViewModel,
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val predictionsRepository: IPredictionsRepository,
    private val sentryRepository: ISentryRepository,
    private val schedulesRepository: ISchedulesRepository,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) :
    MoleculeViewModel<StopDetailsViewModel.Event, StopDetailsViewModel.State>(),
    IStopDetailsViewModel {
    public sealed class Event {
        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) :
            Event()
    }

    public data class State(
        val routeData: RouteData? = null,
        val alertSummaries: Map<String, AlertSummary?> = emptyMap(),
        val awaitingPredictionsAfterBackground: Boolean = false,
    )

    public sealed class RouteData {
        public data class Filtered(
            val filteredWith: StopDetailsPageFilters,
            val stopData: RouteCardData.RouteStopData,
        ) : RouteData()

        public data class Unfiltered(
            val filteredWith: StopDetailsPageFilters,
            val routeCards: List<RouteCardData>,
        ) : RouteData()

        public val filters: StopDetailsPageFilters
            get() =
                when (this) {
                    is Filtered -> this.filteredWith
                    is Unfiltered -> this.filteredWith
                }
    }

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setAlertSummariesState")
    private var alertSummaries by mutableStateOf<Map<String, AlertSummary?>>(emptyMap())
    @set:JvmName("setFiltersState")
    private var filters by mutableStateOf<StopDetailsPageFilters?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var routeData: RouteData? by remember { mutableStateOf(null) }
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }

        var active: Boolean by remember { mutableStateOf(true) }

        val errorKey = "StopDetailsViewModel"
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

        val schedules =
            getSchedules(
                stopIds,
                errorKey,
                schedulesRepository,
                errorBannerRepository,
                coroutineDispatcher,
            )

        val predictions =
            subscribeToPredictions(
                stopIds,
                active,
                errorKey,
                onAnyMessageReceived = { awaitingPredictionsAfterBackground = false },
                errorBannerRepository,
                predictionsRepository,
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

        LaunchedEffect(stopIds, globalData, schedules, predictions, alerts, now, filters) {
            val resolvedFilters = filters
            if (
                stopIds == null ||
                    globalData == null ||
                    schedules == null ||
                    predictions == null ||
                    resolvedFilters == null
            ) {
                routeCardData = null
                routeData = null
                return@LaunchedEffect
            } else if (routeData?.filters?.stopId != filters?.stopId) {
                routeCardData = null
                routeData = null
            }

            routeCardData =
                RouteCardData.routeCardsForStopList(
                    stopIds,
                    globalData,
                    sortByDistanceFrom = null,
                    schedules,
                    predictions,
                    alerts,
                    now,
                    if (resolvedFilters.stopFilter != null)
                        RouteCardData.Context.StopDetailsFiltered
                    else RouteCardData.Context.StopDetailsUnfiltered,
                )

            routeData =
                when {
                    routeCardData?.hasContext(RouteCardData.Context.StopDetailsFiltered) == true ->
                        routeCardData
                            ?.find { it.lineOrRoute.id == resolvedFilters.stopFilter?.routeId }
                            ?.stopData
                            ?.firstOrNull()
                            ?.let { RouteData.Filtered(resolvedFilters, it) }

                    routeCardData?.hasContext(RouteCardData.Context.StopDetailsUnfiltered) ==
                        true -> routeCardData?.let { RouteData.Unfiltered(resolvedFilters, it) }

                    else -> null
                }
        }

        fun applyFilterUpdate(
            routeCardData: List<RouteCardData>?,
            currentFilters: StopDetailsPageFilters,
            stopDetailsFilterOverride: StopDetailsFilter? = null,
        ) {
            val autoTripFilter =
                StopDetailsUtils.autoTripFilter(
                    routeCardData,
                    stopDetailsFilterOverride ?: currentFilters.stopFilter,
                    currentFilters.tripFilter,
                    now,
                    globalData,
                )

            if (
                autoTripFilter != currentFilters.tripFilter ||
                    (stopDetailsFilterOverride != null &&
                        stopDetailsFilterOverride != currentFilters.stopFilter)
            ) {
                _filterUpdates.tryEmit(
                    StopDetailsPageFilters(
                        currentFilters.stopId,
                        stopDetailsFilterOverride ?: currentFilters.stopFilter,
                        autoTripFilter,
                    )
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

            val autoStopFilter =
                resolvedFilters.stopFilter ?: StopDetailsUtils.autoStopFilter(resolvedRouteCardData)
            applyFilterUpdate(resolvedRouteCardData, resolvedFilters, autoStopFilter)
        }

        val state =
            remember(filters, routeData, alertSummaries, awaitingPredictionsAfterBackground) {
                State(routeData, alertSummaries, awaitingPredictionsAfterBackground)
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

    override fun setAlertSummaries(alertSummaries: Map<String, AlertSummary?>): Unit {
        this.alertSummaries = alertSummaries
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

public class MockStopDetailsViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: StopDetailsViewModel.State = StopDetailsViewModel.State()) :
    IStopDetailsViewModel {

    public var onSetActive: (active: Boolean, wasSentToBackground: Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (alerts: AlertsStreamDataResponse?) -> Unit = {}
    public var onSetAlertSummaries: (alertSummaries: Map<String, AlertSummary?>) -> Unit = {}
    public var onSetFilters: (filters: StopDetailsPageFilters) -> Unit = {}
    public var onSetNow: (now: EasternTimeInstant) -> Unit = {}

    override val models: MutableStateFlow<StopDetailsViewModel.State> =
        MutableStateFlow(initialState)
    override val filterUpdates: MutableStateFlow<StopDetailsPageFilters?> = MutableStateFlow(null)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        onSetActive(active, wasSentToBackground)

    override fun setAlerts(alerts: AlertsStreamDataResponse?): Unit = onSetAlerts(alerts)

    override fun setAlertSummaries(alertSummaries: Map<String, AlertSummary?>): Unit =
        onSetAlertSummaries(alertSummaries)

    override fun setFilters(filters: StopDetailsPageFilters): Unit = onSetFilters(filters)

    override fun setNow(now: EasternTimeInstant): Unit = onSetNow(now)
}
