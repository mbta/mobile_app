package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.routes.SheetRoutes
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

    public fun setContextualTrip(upcomingTrip: UpcomingTrip?)

    public fun setFilter(filter: TripDetailsPageFilter?)

    public fun setNow(now: EasternTimeInstant)
}

public class TripDetailsPageViewModel(private val tripDetailsVM: ITripDetailsViewModel) :
    MoleculeViewModel<TripDetailsPageViewModel.Event, TripDetailsPageViewModel.State>(),
    ITripDetailsPageViewModel {
    public sealed class Event

    public data class State(val direction: Direction?, val trip: Trip?)

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setContextualTripState")
    private var contextualTrip by mutableStateOf<UpcomingTrip?>(null)
    @set:JvmName("setFilterState")
    private var filter by mutableStateOf<TripDetailsPageFilter?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        val global =
            getGlobalData(ErrorKey(setOf(SheetRoutes.TripDetails::class), "TripDetailsPage"))

        val tripDetailsState by tripDetailsVM.models.collectAsState()
        val trip = tripDetailsState.tripData?.trip

        val route = global?.getRoute(trip?.routeId ?: filter?.routeId as? Route.Id)

        LaunchedEffect(alerts) { tripDetailsVM.setAlerts(alerts) }
        LaunchedEffect(filter) { tripDetailsVM.setFilters(filter) }

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

        return State(direction, trip)
    }

    override val models: StateFlow<State>
        get() = internalModels

    override var koinScope: Scope? = null

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setContextualTrip(upcomingTrip: UpcomingTrip?) {
        this.contextualTrip = upcomingTrip
    }

    override fun setFilter(filter: TripDetailsPageFilter?) {
        this.filter = filter
    }

    override fun setNow(now: EasternTimeInstant) {
        this.now = now
    }
}

public class MockTripDetailsPageViewModel(
    initialState: TripDetailsPageViewModel.State = TripDetailsPageViewModel.State(null, null)
) : ITripDetailsPageViewModel {
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    public var onSetContextualTrip: (UpcomingTrip?) -> Unit = {}
    public var onSetFilter: (TripDetailsPageFilter?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant?) -> Unit = {}

    override val models: StateFlow<TripDetailsPageViewModel.State> = MutableStateFlow(initialState)

    override var koinScope: Scope? = null

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        onSetAlerts(alerts)
    }

    override fun setContextualTrip(upcomingTrip: UpcomingTrip?) {
        onSetContextualTrip(upcomingTrip)
    }

    override fun setFilter(filter: TripDetailsPageFilter?) {
        onSetFilter(filter)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }
}
