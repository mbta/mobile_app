package com.mbta.tid.mbta_app.android.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.SearchInput
import com.mbta.tid.mbta_app.android.search.results.RouteResultsView
import com.mbta.tid.mbta_app.android.search.results.StopResultsView
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.SearchViewModel
import org.koin.androidx.compose.koinViewModel

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(
    expanded: Boolean,
    showSearchBar: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStopNavigation: (stopId: String) -> Unit,
    onRouteNavigation: (routeId: String) -> Unit,
    inputFieldFocusRequester: FocusRequester,
    searchVM: SearchViewModel = koinViewModel(),
    onBarGloballyPositioned: (LayoutCoordinates) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var searchInputState by rememberSaveable { mutableStateOf("") }
    val searchVMState by searchVM.models.collectAsState()
    val includeRoutes = SettingsCache.get(Settings.SearchRouteResults)

    LaunchedEffect(searchInputState) { searchVM.setQuery(searchInputState) }
    LaunchedEffect(showSearchBar, expanded) {
        if (showSearchBar) {
            searchVM.refreshHistory()
            onExpandedChange(expanded)
            if (!expanded) {
                searchInputState = ""
            }
        }
    }

    Box(contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.zIndex(1f), contentAlignment = Alignment.Center) {
            if (showSearchBar) {
                SearchBar(
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        SearchBarDefaults.colors(
                            containerColor =
                                if (expanded) colorResource(R.color.fill1) else Color.Transparent
                        ),
                    inputField = {
                        SearchInput(
                            searchInputState,
                            { searchInputState = it },
                            expanded,
                            onExpandedChange,
                            inputFieldFocusRequester,
                            onBarGloballyPositioned,
                        ) {
                            Text(stringResource(R.string.stops), style = Typography.callout)
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(colorResource(R.color.fill1)),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        if (searchInputState.isEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    text = stringResource(R.string.recently_viewed),
                                    style = Typography.subheadlineSemibold,
                                )
                            }
                        }
                        val (stopResults, routeResults) =
                            when (val state = searchVMState) {
                                SearchViewModel.State.Loading -> Pair(null, null)
                                is SearchViewModel.State.RecentStops -> Pair(state.stops, null)
                                is SearchViewModel.State.Results -> Pair(state.stops, state.routes)
                                SearchViewModel.State.Error -> Pair(null, null)
                            }
                        itemsIndexed(stopResults ?: emptyList()) { index, stopResult ->
                            val shape =
                                if (stopResults?.size == 1) {
                                    RoundedCornerShape(10.dp)
                                } else if (stopResults?.first() == stopResult) {
                                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                } else if (stopResults?.last() == stopResult) {
                                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                } else {
                                    RoundedCornerShape(0.dp)
                                }
                            if (index != 0) {
                                HorizontalDivider(color = colorResource(R.color.fill1))
                            }
                            StopResultsView(shape, stopResult, onStopNavigation)
                        }
                        if (includeRoutes && !routeResults.isNullOrEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.routes),
                                    Modifier.padding(vertical = 8.dp),
                                    style = Typography.subheadlineSemibold,
                                )
                            }
                            items(routeResults) { routeResult ->
                                val shape =
                                    if (routeResults.size == 1) {
                                        RoundedCornerShape(10.dp)
                                    } else if (routeResults.first() == routeResult) {
                                        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                    } else if (routeResults.last() == routeResult) {
                                        RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                    } else {
                                        RoundedCornerShape(0.dp)
                                    }
                                HorizontalDivider(color = colorResource(R.color.fill1))
                                RouteResultsView(shape, routeResult, onRouteNavigation)
                            }
                        }
                    }
                }
            }
        }

        content()
    }
}
