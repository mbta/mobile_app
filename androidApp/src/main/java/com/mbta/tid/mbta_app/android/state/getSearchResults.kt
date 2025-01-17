package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.analytics.AnalyticsProvider
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SearchResultsViewModel(
    private val globalResponse: GlobalResponse?,
    private val analytics: Analytics,
    private val searchResultRepository: ISearchResultRepository,
    private val visitHistoryUsecase: VisitHistoryUsecase,
) : ViewModel() {
    private var _searchResults: MutableStateFlow<SearchResults?> = MutableStateFlow(null)
    private var job: Job? = null
    val searchResults: StateFlow<SearchResults?> = _searchResults

    fun getSearchResults(query: String) {
        analytics.performedSearch(query)
        job?.cancel()
        job =
            CoroutineScope(Dispatchers.IO).launch {
                if (query.isNotEmpty()) {
                    delay(500)
                    when (val data = searchResultRepository.getSearchResults(query)) {
                        is ApiResult.Ok -> _searchResults.emit(data.data)
                        is ApiResult.Error -> _searchResults.emit(null)
                        null -> {}
                    }
                } else {
                    val latestVisits = visitHistoryUsecase.getLatestVisits()
                    _searchResults.emit(
                        SearchResults(
                            stops = latestVisits.mapIndexedNotNull(::mapLatestVisitsToResult),
                            routes = emptyList(),
                        )
                    )
                }
            }
    }

    private fun mapLatestVisitsToResult(index: Int, visit: Visit): StopResult? {
        if (visit is Visit.StopVisit && globalResponse != null) {
            val stop = globalResponse.stops[visit.stopId]
            if (stop == null) {
                return null
            }

            val routes = globalResponse.getTypicalRoutesFor(stop.id)
            val isStation = stop.locationType == LocationType.STATION
            return StopResult(
                id = stop.id,
                name = stop.name,
                isStation = isStation,
                routes = routes.map { StopResultRoute(type = it.type, icon = "") },
                rank = index,
                zone = null,
            )
        } else {
            return null
        }
    }
}

@Composable
fun getSearchResultsVm(
    globalResponse: GlobalResponse?,
    analytics: Analytics = AnalyticsProvider.shared,
    searchResultRepository: ISearchResultRepository = koinInject(),
    visitHistoryUsecase: VisitHistoryUsecase = koinInject()
): SearchResultsViewModel {
    val viewModel =
        remember(globalResponse) {
            SearchResultsViewModel(
                globalResponse,
                analytics,
                searchResultRepository,
                visitHistoryUsecase
            )
        }
    return viewModel
}
