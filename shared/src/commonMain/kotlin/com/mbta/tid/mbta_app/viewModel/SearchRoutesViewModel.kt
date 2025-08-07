package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.silverRoutes
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

interface ISearchRoutesViewModel {

    val models: StateFlow<SearchRoutesViewModel.State>

    fun setPath(path: RoutePickerPath)

    fun setQuery(query: String)
}

class SearchRoutesViewModel(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
) :
    MoleculeViewModel<SearchRoutesViewModel.Event, SearchRoutesViewModel.State>(),
    ISearchRoutesViewModel {
    sealed interface Event {
        data class SetPath(val path: RoutePickerPath) : Event

        data class SetQuery(val query: String) : Event
    }

    sealed class State {
        data object Unfiltered : State()

        data class Results(val routeIds: List<String>) : State()

        data object Error : State()

        val isEmpty: Boolean
            get() =
                when (this) {
                    Unfiltered -> false
                    is Results -> routeIds.isEmpty()
                    Error -> false
                }
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var path: RoutePickerPath? by remember { mutableStateOf(null) }
        var query by remember { mutableStateOf("") }

        var state by remember { mutableStateOf<State>(State.Unfiltered) }

        LaunchedEffect(null) { globalRepository.getGlobalData() }

        LaunchedEffect(null) {
            events.collect { event ->
                when (event) {
                    is Event.SetPath -> path = event.path
                    is Event.SetQuery -> query = event.query
                }
            }
        }

        LaunchedEffect(path) { setQuery("") }

        LaunchedEffect(query) {
            analytics.performedRouteFilter(query)
            if (query.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    delay(500)
                    val params = getParamsForPath(path)
                    when (
                        val data =
                            searchResultRepository.getRouteFilterResults(
                                query,
                                params.lineIds,
                                params.routeTypes,
                            )
                    ) {
                        is ApiResult.Ok ->
                            state =
                                State.Results(
                                    data.data.routes
                                        .map {
                                            when {
                                                it.id == "Green" -> "line-Green"
                                                else -> it.id
                                            }
                                        }
                                        .sortedBy {
                                            when (path) {
                                                is RoutePickerPath.Bus ->
                                                    if (it in silverRoutes) 1 else 0
                                                else -> 0
                                            }
                                        }
                                )
                        is ApiResult.Error -> {
                            // Only set to error if there's a backend error code, otherwise this
                            // can catch exceptions from debounced LaunchedEffect cancellation
                            if (data.code != null) state = State.Error
                        }
                        null -> {}
                    }
                }
            } else {
                state = State.Unfiltered
            }
        }

        return state
    }

    override val models
        get() = internalModels

    override fun setPath(path: RoutePickerPath) = fireEvent(Event.SetPath(path))

    override fun setQuery(query: String) = fireEvent(Event.SetQuery(query))

    companion object {
        data class FilterParams(val lineIds: List<String>?, val routeTypes: List<RouteType>?)

        fun getParamsForPath(path: RoutePickerPath?) =
            when (path) {
                RoutePickerPath.Bus -> FilterParams(null, listOf(RouteType.BUS))
                RoutePickerPath.CommuterRail -> FilterParams(null, listOf(RouteType.COMMUTER_RAIL))
                RoutePickerPath.Ferry -> FilterParams(null, listOf(RouteType.FERRY))
                RoutePickerPath.Root,
                null -> FilterParams(null, null)
                RoutePickerPath.Silver ->
                    FilterParams(
                        listOf("line-SLWaterfront", "line-SLWashington"),
                        listOf(RouteType.BUS),
                    )
            }
    }
}

class MockSearchRoutesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: SearchRoutesViewModel.State = SearchRoutesViewModel.State.Unfiltered) :
    ISearchRoutesViewModel {
    var onSetPath = { _: RoutePickerPath -> }
    var onSetQuery = { _: String -> }

    override val models = MutableStateFlow(initialState)

    override fun setPath(path: RoutePickerPath) = onSetPath(path)

    override fun setQuery(query: String) = onSetQuery(query)
}
