package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.SearchViewModel

@Composable
fun SearchResultsView(
    state: SearchViewModel.State,
    handleStopTap: (String) -> Unit,
    handleRouteTap: (LineOrRoute.Id) -> Unit,
) {
    val includeRoutes = SettingsCache.get(Settings.SearchRouteResults)
    Column(
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .background(colorResource(R.color.fill1))
                .padding(16.dp)
                .navigationBarsPadding()
    ) {
        when (state) {
            SearchViewModel.State.Loading -> LoadingResultsView()
            is SearchViewModel.State.RecentStops -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        modifier = Modifier.padding(bottom = 10.dp),
                        text = stringResource(R.string.recently_viewed),
                        style = Typography.subheadlineSemibold,
                    )
                    StopResultsView(state.stops, handleStopTap)
                }
            }
            is SearchViewModel.State.Results -> {
                if (state.isEmpty(includeRoutes)) {
                    EmptyStateView(
                        headline = stringResource(R.string.no_results_found),
                        subheadline = stringResource(R.string.try_a_different_spelling_or_name),
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StopResultsView(state.stops, handleStopTap)
                        if (includeRoutes && !state.routes.isEmpty()) {
                            RouteResultsView(state.routes, handleRouteTap)
                        }
                    }
                }
            }
            SearchViewModel.State.Error ->
                EmptyStateView(
                    headline = stringResource(R.string.results_failed_to_load),
                    subheadline = stringResource(R.string.try_your_search_again),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                )
        }
    }
}
