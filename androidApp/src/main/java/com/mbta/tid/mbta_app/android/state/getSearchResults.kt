package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import org.koin.compose.koinInject

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SearchResultsViewModel(
    private val searchResultRepository: ISearchResultRepository,
    private val query: String
) : ViewModel() {
    var searchResults: Flow<SearchResults?>

    init {
        searchResults =
            snapshotFlow { query }
                .flowOn(Dispatchers.IO)
                .debounce(500L)
                .distinctUntilChanged()
                .transform { query ->
                    if (query.isBlank()) {
                        emit(null)
                    } else {
                        emit(getSearchResults(query))
                    }
                }
    }

    private suspend fun getSearchResults(query: String): SearchResults? {
        return when (val data = searchResultRepository.getSearchResults(query)) {
            is ApiResult.Ok -> data.data
            is ApiResult.Error -> null
            null -> null
        }
    }
}

@Composable
fun getSearchResults(
    query: String,
    searchResultRepository: ISearchResultRepository = koinInject()
): SearchResults? {
    val viewModel = remember(query) { SearchResultsViewModel(searchResultRepository, query) }
    return viewModel.searchResults.collectAsState(initial = null).value
}
