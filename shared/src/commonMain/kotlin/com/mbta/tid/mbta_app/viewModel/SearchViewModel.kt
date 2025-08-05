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
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

interface ISearchViewModel {

    val models: StateFlow<SearchViewModel.State>

    fun setQuery(query: String)

    fun refreshHistory()
}

class SearchViewModel(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
    private val visitHistoryUsecase: VisitHistoryUsecase,
) : MoleculeViewModel<SearchViewModel.Event, SearchViewModel.State>(), ISearchViewModel {
    sealed interface Event {
        data class SetQuery(val query: String) : Event

        data object RefreshHistory : Event
    }

    sealed class State {
        data object Loading : State()

        data class RecentStops(val stops: List<StopResult>) : State()

        data class Results(val stops: List<StopResult>, val routes: List<RouteResult>) : State()

        data object Error : State()

        fun isEmpty(includeRoutes: Boolean): Boolean =
            when (this) {
                Loading -> false
                is RecentStops -> false
                is Results -> stops.isEmpty() && !(includeRoutes && routes.isNotEmpty())
                Error -> false
            }
    }

    data class StopResult(
        val id: String,
        val isStation: Boolean,
        val name: String,
        val routePills: List<RoutePillSpec>,
    ) {
        companion object {
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

    data class RouteResult(val id: String, val name: String, val routePill: RoutePillSpec) {
        companion object {
            fun forLineOrRoute(objectId: String, globalData: GlobalResponse?): RouteResult? {
                val route = globalData?.getRoute(objectId)
                val line = globalData?.getLine(objectId)

                val routePillSpec =
                    RoutePillSpec(
                        route,
                        line,
                        RoutePillSpec.Type.Fixed,
                        RoutePillSpec.Context.Default,
                    )

                return RouteResult(
                    id = objectId,
                    name = line?.longName ?: route?.longName ?: return null,
                    routePill = routePillSpec,
                )
            }
        }
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        val globalData by globalRepository.state.collectAsState()
        var latestVisits by remember { mutableStateOf<List<Visit>?>(null) }
        var query by remember { mutableStateOf("") }
        var state by remember { mutableStateOf<State>(State.Loading) }

        LaunchedEffect(null) { globalRepository.getGlobalData() }

        LaunchedEffect(null) { latestVisits = visitHistoryUsecase.getLatestVisits() }

        LaunchedEffect(null) {
            events.collect { event ->
                when (event) {
                    is Event.SetQuery -> query = event.query
                    Event.RefreshHistory -> latestVisits = visitHistoryUsecase.getLatestVisits()
                }
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

    override val models
        get() = internalModels

    override fun setQuery(query: String) = fireEvent(Event.SetQuery(query))

    override fun refreshHistory() = fireEvent(Event.RefreshHistory)
}

class MockSearchViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: SearchViewModel.State = SearchViewModel.State.Loading) :
    ISearchViewModel {
    var onSetQuery = { _: String -> }
    var onRefreshHistory = {}
    override val models = MutableStateFlow(initialState)

    override fun setQuery(query: String) {
        onSetQuery(query)
    }

    override fun refreshHistory() {
        onRefreshHistory()
    }
}
