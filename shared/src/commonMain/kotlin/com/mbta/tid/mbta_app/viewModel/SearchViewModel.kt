package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.silverRoutes
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

private fun stopRouteContentDescription(
    isStation: Boolean,
    route: Route,
): RoutePillSpec.ContentDescription.StopSearchResultRoute {
    if (silverRoutes.contains(route.id) && isStation) {
        val routeName = "Silver Line"
        return RoutePillSpec.ContentDescription.StopSearchResultRoute(
            routeName,
            route.type,
            isOnly = false,
        )
    } else if (route.type == RouteType.COMMUTER_RAIL && isStation) {
        val routeName = "Commuter Rail"
        return RoutePillSpec.ContentDescription.StopSearchResultRoute(
            routeName,
            route.type,
            isOnly = false,
        )
    } else if (route.type == RouteType.BUS && isStation) {
        return RoutePillSpec.ContentDescription.StopSearchResultRoute(
            routeName = null,
            route.type,
            isOnly = false,
        )
    } else {
        return RoutePillSpec.ContentDescription.StopSearchResultRoute(
            route.label,
            route.type,
            isOnly = true,
        )
    }
}

public interface ISearchViewModel {

    public val models: StateFlow<SearchViewModel.State>

    public fun setQuery(query: String)

    public fun refreshHistory()
}

public class SearchViewModel(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
    private val sentryRepository: ISentryRepository,
    private val visitHistoryUsecase: VisitHistoryUsecase,
) : MoleculeViewModel<SearchViewModel.Event, SearchViewModel.State>(), ISearchViewModel {
    public sealed interface Event {
        public data object RefreshHistory : Event
    }

    public sealed class State {
        public data object Loading : State()

        public data class RecentStops(val stops: List<StopResult>) : State()

        public data class Results(val stops: List<StopResult>, val routes: List<RouteResult>) :
            State()

        public data object Error : State()

        public fun isEmpty(includeRoutes: Boolean): Boolean =
            when (this) {
                Loading -> false
                is RecentStops -> false
                is Results -> stops.isEmpty() && !(includeRoutes && routes.isNotEmpty())
                Error -> false
            }
    }

    public data class StopResult(
        val id: String,
        val isStation: Boolean,
        val name: String,
        val routePills: List<RoutePillSpec>,
    ) {
        internal companion object {
            fun forStop(stopId: String, globalData: GlobalResponse?): StopResult? {
                val stop = globalData?.getStop(stopId) ?: return null
                val isStation = stop.locationType == LocationType.STATION
                val routes = globalData.getTypicalRoutesFor(stopId)
                val routePills =
                    routes.sorted().map { route ->
                        val line: Line? = globalData.getLine(route.lineId)
                        val context: RoutePillSpec.Context =
                            if (isStation) RoutePillSpec.Context.SearchStation
                            else RoutePillSpec.Context.Default
                        RoutePillSpec(
                            route,
                            line,
                            RoutePillSpec.Type.FlexCompact,
                            RoutePillSpec.Height.Small,
                            context,
                            stopRouteContentDescription(isStation, route),
                        )
                    }

                return StopResult(
                    id = stop.id,
                    isStation = isStation,
                    name = stop.name,
                    routePills = routePills.distinct(),
                )
            }
        }
    }

    public data class RouteResult(val id: String, val name: String, val routePill: RoutePillSpec) {
        public companion object {
            public fun forLineOrRoute(objectId: String, globalData: GlobalResponse?): RouteResult? {
                val route = globalData?.getRoute(objectId)
                val line = globalData?.getLine(objectId)

                val routePillSpec =
                    RoutePillSpec(
                        route,
                        line,
                        RoutePillSpec.Type.Fixed,
                        context = RoutePillSpec.Context.Default,
                    )

                return RouteResult(
                    id = objectId,
                    name = line?.longName ?: route?.longName ?: return null,
                    routePill = routePillSpec,
                )
            }
        }
    }

    @set:JvmName("setQueryState") private var query by mutableStateOf("")

    @Composable
    override fun runLogic(): State {
        val globalData by globalRepository.state.collectAsState()
        var latestVisits by remember { mutableStateOf<List<Visit>?>(null) }
        var state by remember { mutableStateOf<State>(State.Loading) }

        LaunchedEffect(null) { globalRepository.getGlobalData() }

        LaunchedEffect(null) { latestVisits = visitHistoryUsecase.getLatestVisits() }

        EventSink(eventHandlingTimeout = 2.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                Event.RefreshHistory -> latestVisits = visitHistoryUsecase.getLatestVisits()
            }
        }

        LaunchedEffect(query, latestVisits, globalData) {
            analytics.performedSearch(query)
            if (query.isNotEmpty()) {
                // explicitly not in dependencies to avoid thrashing every frame
                if (state is State.RecentStops) state = State.Loading
                withContext(Dispatchers.IO) {
                    delay(500)
                    when (val data = searchResultRepository.getSearchResults(query)) {
                        is ApiResult.Ok ->
                            state =
                                State.Results(
                                    data.data.stops.mapNotNull {
                                        StopResult.forStop(it.id, globalData)
                                    },
                                    data.data.routes.mapNotNull {
                                        RouteResult.forLineOrRoute(
                                            when {
                                                it.id == "Green" -> "line-Green"
                                                else -> it.id
                                            },
                                            globalData,
                                        )
                                    },
                                )
                        is ApiResult.Error -> state = State.Error
                        null -> {}
                    }
                }
            } else {
                val latestVisitResults =
                    latestVisits
                        ?.filterIsInstance<Visit.StopVisit>()
                        ?.mapNotNull { StopResult.forStop(it.stopId, globalData) }
                        ?.takeIf { it.isNotEmpty() }
                state =
                    if (latestVisitResults != null) State.RecentStops(latestVisitResults)
                    else State.Loading
            }
        }

        return state
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun setQuery(query: String) {
        this.query = query
    }

    override fun refreshHistory(): Unit = fireEvent(Event.RefreshHistory)
}

public class MockSearchViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: SearchViewModel.State = SearchViewModel.State.Loading) :
    ISearchViewModel {
    public var onSetQuery: (String) -> Unit = {}
    internal var onRefreshHistory = {}
    override val models: MutableStateFlow<SearchViewModel.State> = MutableStateFlow(initialState)

    override fun setQuery(query: String) {
        onSetQuery(query)
    }

    override fun refreshHistory() {
        onRefreshHistory()
    }
}
