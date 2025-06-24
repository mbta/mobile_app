package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.ErrorCard
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.SearchInput
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Bus.routeType
import com.mbta.tid.mbta_app.viewModel.SearchRoutesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoutePickerView(
    path: RoutePickerPath,
    context: RouteDetailsContext,
    onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Unit,
    onOpenRouteDetails: (String, RouteDetailsContext) -> Unit,
    onRouteSearchExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
    searchRoutesViewModel: SearchRoutesViewModel = koinViewModel(),
) {
    val globalData = getGlobalData("RoutePickerView.globalData")
    var searchInputState by rememberSaveable { mutableStateOf("") }
    var searchInputFocused by rememberSaveable { mutableStateOf(false) }
    val searchVMState by searchRoutesViewModel.models.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val routeScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchInputState) { searchRoutesViewModel.setQuery(searchInputState) }

    if (globalData == null) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" })
        return
    }

    val modes =
        listOf(
            RoutePickerPath.Bus,
            RoutePickerPath.Silver,
            RoutePickerPath.CommuterRail,
            RoutePickerPath.Ferry,
        )
    val routes = remember(globalData, path) { globalData.getRoutesForPicker(path) }

    Column(Modifier.fillMaxWidth(), Arrangement.Top, Alignment.CenterHorizontally) {
        Row(
            Modifier.heightIn(min = 48.dp)
                .padding(start = 24.dp, top = 0.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Row(Modifier, Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
                if (path !is RoutePickerPath.Root)
                    ActionButton(
                        ActionButtonKind.Back,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.text).copy(alpha = 0.6f),
                                contentColor = colorResource(R.color.fill2),
                            ),
                        action = onBack,
                    )
                Text(
                    when (path) {
                        is RoutePickerPath.Root ->
                            when (context) {
                                is RouteDetailsContext.Favorites ->
                                    stringResource(R.string.route_picker_header_favorites)

                                is RouteDetailsContext.Details -> TODO("Implement details header")
                            }

                        is RoutePickerPath.Bus -> stringResource(R.string.bus)
                        is RoutePickerPath.Silver -> "Silver Line"
                        is RoutePickerPath.CommuterRail -> "Commuter Rail"
                        is RoutePickerPath.Ferry -> stringResource(R.string.ferry)
                    },
                    style = Typography.title2Bold,
                    color = path.textColor,
                )
            }
            NavTextButton(stringResource(R.string.done), onTap = onClose)
        }
        ErrorBanner(errorBannerViewModel, Modifier.padding(start = 14.dp, top = 6.dp, end = 14.dp))
        AnimatedVisibility(searchVMState is SearchRoutesViewModel.State.Error) {
            ErrorCard(
                Modifier.padding(bottom = 16.dp),
                details = {
                    Text(
                        stringResource(R.string.error_loading_data),
                        style = Typography.subheadline,
                    )
                },
            )
        }

        if (path != RoutePickerPath.Root) {
            SearchInput(
                searchInputState,
                { searchInputState = it },
                searchInputFocused,
                {
                    if (!it) {
                        // Only reset the search result scroll position on clear if there is an
                        // active search, to prevent autoscrolling an already unfiltered list
                        if (searchInputState.isNotEmpty())
                            scope.launch { routeScroll.animateScrollTo(0) }
                        searchInputState = ""
                    }
                    onRouteSearchExpandedChange(it)
                    searchInputFocused = it
                },
                searchFocusRequester,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Text(stringResource(R.string.filter_routes), style = Typography.callout)
            }
        }
        ScrollSeparatorColumn(
            Modifier.imePadding().padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 26.dp),
            Arrangement.spacedBy(4.dp),
            haloColor = path.haloColor,
            scrollState = routeScroll,
        ) {
            if (path == RoutePickerPath.Root) {
                for (mode in modes) {
                    RoutePickerRootRow(mode) { onOpenPickerPath(mode, context) }
                }
                Text(
                    stringResource(R.string.subway),
                    style = Typography.subheadlineSemibold,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 2.dp),
                )
                for (route in routes) {
                    RoutePickerRootRow(route) { onOpenRouteDetails(route.id, context) }
                }
            } else {
                val displayedRoutes =
                    remember(routes, searchVMState) {
                        when (val state = searchVMState) {
                            is SearchRoutesViewModel.State.Unfiltered,
                            is SearchRoutesViewModel.State.Error -> routes

                            is SearchRoutesViewModel.State.Results -> {
                                state.routeIds.mapNotNull {
                                    routes.firstOrNull { route -> route.id == it }
                                }
                            }
                        }
                    }
                if (displayedRoutes.isNotEmpty())
                    Column(
                        Modifier.padding(bottom = 14.dp)
                            .haloContainer(2.dp, outlineColor = path.haloColor)
                    ) {
                        for ((index, route) in displayedRoutes.withIndex()) {
                            RoutePickerRow(route) { onOpenRouteDetails(route.id, context) }
                            if (index < displayedRoutes.lastIndex) {
                                HaloSeparator()
                            }
                        }
                    }
                if (searchVMState is SearchRoutesViewModel.State.Results)
                    Column(
                        Modifier.padding(top = 6.dp).fillMaxWidth(),
                        Arrangement.spacedBy(2.dp),
                        Alignment.CenterHorizontally,
                    ) {
                        if (displayedRoutes.isEmpty())
                            Text(
                                stringResource(
                                    R.string.no_matching_routes,
                                    path.routeType.typeText(LocalContext.current, true),
                                ),
                                color = path.textColor,
                                style = Typography.bodySemibold,
                            )
                        Text(
                            stringResource(R.string.find_stops_hint),
                            color = path.textColor,
                            style = Typography.body,
                        )
                    }
            }
        }
    }
}
