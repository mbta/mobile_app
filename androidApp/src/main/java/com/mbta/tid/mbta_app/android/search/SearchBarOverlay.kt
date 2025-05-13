package com.mbta.tid.mbta_app.android.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.search.results.RouteResultsView
import com.mbta.tid.mbta_app.android.search.results.StopResultsView
import com.mbta.tid.mbta_app.android.state.SearchResultsViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.repositories.Settings

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(
    expanded: Boolean,
    showSearchBar: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStopNavigation: (stopId: String) -> Unit,
    onRouteNavigation: (routeId: String) -> Unit,
    inputFieldFocusRequester: FocusRequester,
    searchResultsVm: SearchResultsViewModel,
    content: @Composable () -> Unit,
) {
    var searchInputState by rememberSaveable { mutableStateOf("") }
    val globalResponse = getGlobalData("SearchBar.getGlobalData")
    val searchResults = searchResultsVm.searchResults.collectAsState(initial = null).value
    val includeRoutes = SettingsCache.get(Settings.SearchRouteResults)

    val buttonColors =
        ButtonColors(
            containerColor = colorResource(R.color.fill3),
            disabledContainerColor = colorResource(R.color.fill3),
            contentColor = colorResource(R.color.deemphasized),
            disabledContentColor = colorResource(R.color.deemphasized),
        )
    LaunchedEffect(searchInputState, showSearchBar, globalResponse) {
        searchResultsVm.getSearchResults(searchInputState, globalResponse)
    }
    LaunchedEffect(showSearchBar, expanded) {
        if (showSearchBar) {
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
                        SearchBarDefaults.InputField(
                            colors =
                                SearchBarDefaults.inputFieldColors(
                                    focusedTextColor = colorResource(R.color.text),
                                    unfocusedTextColor = colorResource(R.color.text),
                                    focusedPlaceholderColor = colorResource(R.color.deemphasized),
                                    unfocusedPlaceholderColor = colorResource(R.color.deemphasized),
                                ),
                            query = searchInputState,
                            placeholder = {
                                Text(
                                    stringResource(R.string.stops),
                                    // This will be drawn in bodyLarge if we don't
                                    // re-override it here
                                    style = Typography.callout,
                                )
                            },
                            expanded = expanded,
                            onQueryChange = { searchInputState = it },
                            onExpandedChange = onExpandedChange,
                            modifier =
                                Modifier.padding(horizontal = 14.dp)
                                    .haloContainer(
                                        2.dp,
                                        borderRadius = 8.dp,
                                        backgroundColor = colorResource(R.color.fill3),
                                    )
                                    .fillMaxWidth()
                                    .focusRequester(inputFieldFocusRequester),
                            onSearch = {},
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.magnifying_glass),
                                    null,
                                    tint = colorResource(R.color.deemphasized),
                                )
                            },
                            trailingIcon = {
                                if (expanded) {
                                    Button(
                                        colors = buttonColors,
                                        onClick = { onExpandedChange(false) },
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.fa_xmark),
                                            stringResource(R.string.close_button_label),
                                            tint = colorResource(R.color.deemphasized),
                                        )
                                    }
                                }
                            },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = {},
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
                        itemsIndexed(searchResults?.stops ?: emptyList()) { index, stop ->
                            val shape =
                                if (searchResults?.stops?.size == 1) {
                                    RoundedCornerShape(10.dp)
                                } else if (searchResults?.stops?.first() == stop) {
                                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                } else if (searchResults?.stops?.last() == stop) {
                                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                } else {
                                    RoundedCornerShape(0.dp)
                                }
                            if (index != 0) {
                                HorizontalDivider(color = colorResource(R.color.fill1))
                            }
                            StopResultsView(shape, stop, globalResponse, onStopNavigation)
                        }
                        if (includeRoutes && !searchResults?.routes.isNullOrEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.routes),
                                    Modifier.padding(vertical = 8.dp),
                                    style = Typography.subheadlineSemibold,
                                )
                            }
                            items(searchResults?.routes.orEmpty()) { route ->
                                val shape =
                                    if (searchResults?.routes?.size == 1) {
                                        RoundedCornerShape(10.dp)
                                    } else if (searchResults?.routes?.first() == route) {
                                        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                    } else if (searchResults?.routes?.last() == route) {
                                        RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                    } else {
                                        RoundedCornerShape(0.dp)
                                    }
                                HorizontalDivider(color = colorResource(R.color.fill1))
                                RouteResultsView(shape, route, globalResponse, onRouteNavigation)
                            }
                        }
                    }
                }
            }
        }

        content()
    }
}
