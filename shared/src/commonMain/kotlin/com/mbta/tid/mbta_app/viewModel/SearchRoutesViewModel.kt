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
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

public interface ISearchRoutesViewModel {

    public val models: StateFlow<SearchRoutesViewModel.State>

    public fun setPath(path: RoutePickerPath)

    public fun setQuery(query: String)
}

public class SearchRoutesViewModel
internal constructor(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
    private val sentryRepository: ISentryRepository,
) :
    MoleculeViewModel<SearchRoutesViewModel.Event, SearchRoutesViewModel.State>(),
    ISearchRoutesViewModel {
    public sealed interface Event {
        public data class SetPath internal constructor(val path: RoutePickerPath) : Event
    }

    public sealed class State {
        public data object Unfiltered : State()

        public data class Results(val routeIds: List<String>) : State()

        public data object Error : State()

        public val isEmpty: Boolean
            get() =
                when (this) {
                    Unfiltered -> false
                    is Results -> routeIds.isEmpty()
                    Error -> false
                }
    }

    @set:JvmName("setQueryState") private var query by mutableStateOf("")

    @Composable
    override fun runLogic(): State {
        var path: RoutePickerPath? by remember { mutableStateOf(null) }

        var state by remember { mutableStateOf<State>(State.Unfiltered) }

        LaunchedEffect(null) { globalRepository.getGlobalData() }

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                is Event.SetPath -> {
                    path = event.path
                    state = State.Unfiltered
                }
            }
        }

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

    override val models: StateFlow<State>
        get() = internalModels

    override fun setPath(path: RoutePickerPath): Unit = fireEvent(Event.SetPath(path))

    override fun setQuery(query: String) {
        this.query = query
    }

    internal companion object {
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

public class MockSearchRoutesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: SearchRoutesViewModel.State = SearchRoutesViewModel.State.Unfiltered) :
    ISearchRoutesViewModel {
    public var onSetPath: (RoutePickerPath) -> Unit = {}
    public var onSetQuery: (String) -> Unit = {}

    override val models: MutableStateFlow<SearchRoutesViewModel.State> =
        MutableStateFlow(initialState)

    override fun setPath(path: RoutePickerPath): Unit = onSetPath(path)

    override fun setQuery(query: String): Unit = onSetQuery(query)
}
