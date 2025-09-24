package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSignificance
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.discardTrackChangesAtCRCore
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.scope.Scope

public interface ITripDetailsPageViewModel {
    public val models: StateFlow<TripDetailsPageViewModel.State>

    public var koinScope: Scope?

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setFilter(filter: TripDetailsPageFilter?)

    public fun setNow(now: EasternTimeInstant)
}

public class TripDetailsPageViewModel(private val tripDetailsVM: ITripDetailsViewModel) :
    MoleculeViewModel<TripDetailsPageViewModel.Event, TripDetailsPageViewModel.State>(),
    ITripDetailsPageViewModel {
    public sealed class Event

    public data class State(
        val direction: Direction?,
        val alertSummaries: Map<String, AlertSummary?>,
    )

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setFilterState")
    private var filter by mutableStateOf<TripDetailsPageFilter?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        val global = getGlobalData("TripDetailsPage")

        val route = global?.getRoute(filter?.routeId)
        val stop = global?.getStop(filter?.stopId)

        val tripDetailsState by tripDetailsVM.models.collectAsState()
        val trip = tripDetailsState.tripData?.trip

        LaunchedEffect(alerts) { tripDetailsVM.setAlerts(alerts) }
        LaunchedEffect(filter) { tripDetailsVM.setFilters(filter) }

        val patterns =
            remember(global, filter, route, trip) {
                val filter = filter
                val allPatterns =
                    if (filter != null && global != null && route != null)
                        global.getPatternsFor(filter.stopId, RouteCardData.LineOrRoute.Route(route))
                    else emptyList()
                if (trip != null) allPatterns.filter { it.id == trip.routePatternId }
                else if (filter != null) allPatterns.filter { it.directionId == filter.directionId }
                else allPatterns
            }
        val direction =
            remember(route, filter, trip) {
                val filter = filter
                if (route != null && filter != null) {
                    Direction(
                        name = route.directionNames[filter.directionId],
                        destination =
                            trip?.headsign ?: route.directionDestinations[filter.directionId],
                        id = filter.directionId,
                    )
                } else {
                    null
                }
            }

        val alertsToSummarize =
            remember(alerts, filter, patterns, now, global) {
                val alerts = alerts
                val filter = filter
                if (alerts != null && filter != null) {
                    val activeRelevantAlerts =
                        alerts.alerts.values.filter {
                            it.isActive(time = now) && it.significance >= AlertSignificance.Minor
                        }
                    val isCRCore = stop?.isCRCore ?: false
                    Alert.applicableAlerts(
                            activeRelevantAlerts,
                            filter.directionId,
                            listOf(filter.routeId),
                            null,
                            filter.tripId,
                        )
                        .discardTrackChangesAtCRCore(isCRCore)
                } else emptyList()
            }
        var alertSummaries: Map<String, AlertSummary?> by remember { mutableStateOf(emptyMap()) }

        suspend fun updateAlertSummaries(clearExisting: Boolean = false) {
            val filter = filter
            if (global == null || filter == null) return
            if (clearExisting) alertSummaries = emptyMap()

            alertSummaries =
                alertsToSummarize.associate {
                    it.id to it.summary(filter.stopId, filter.directionId, patterns, now, global)
                }
        }

        LaunchedEffect(filter?.stopId, filter?.directionId) {
            updateAlertSummaries(clearExisting = true)
        }
        LaunchedEffect(global, alertsToSummarize, patterns, now) { updateAlertSummaries() }

        return State(direction, alertSummaries)
    }

    override val models: StateFlow<State>
        get() = internalModels

    override var koinScope: Scope? = null

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setFilter(filter: TripDetailsPageFilter?) {
        this.filter = filter
    }

    override fun setNow(now: EasternTimeInstant) {
        this.now = now
    }
}

public class MockTripDetailsPageViewModel(
    initialState: TripDetailsPageViewModel.State = TripDetailsPageViewModel.State(null, emptyMap())
) : ITripDetailsPageViewModel {
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    public var onSetFilter: (TripDetailsPageFilter?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant?) -> Unit = {}

    override val models: StateFlow<TripDetailsPageViewModel.State> = MutableStateFlow(initialState)

    override var koinScope: Scope? = null

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        onSetAlerts(alerts)
    }

    override fun setFilter(filter: TripDetailsPageFilter?) {
        onSetFilter(filter)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }
}
