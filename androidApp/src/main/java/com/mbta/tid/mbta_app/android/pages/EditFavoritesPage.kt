package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.routeCard.LoadingRouteCard
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardContainer
import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.android.favorites.NoFavoritesView
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.datetime.Clock

@Composable
fun EditFavoritesPage(
    global: GlobalResponse?,
    favoritesViewModel: FavoritesViewModel,
    onClose: () -> Unit,
) {
    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)
    val initialState = favoritesViewModel.favorites.orEmpty()
    val favoritesState = remember {
        mutableStateMapOf(*initialState.map { it to true }.toTypedArray())
    }
    var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) { routeCardData = favoritesViewModel.loadStaticRouteCardData(global) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(favoritesViewModel, favoritesState, onClose)
        EditFavoritesList(routeCardData, showStationAccessibility, global) {
            favoritesState[it] = false
            if (global == null) return@EditFavoritesList
            routeCardData =
                favoritesViewModel.filterRouteAndDirection(
                    routeCardData,
                    global,
                    favoritesState.filter { it.value }.keys,
                )
        }
    }
}

@Composable
private fun Header(
    viewModel: FavoritesViewModel,
    currentState: MutableMap<RouteStopDirection, Boolean>?,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.edit_favorites),
            modifier = Modifier.semantics { heading() }.padding(horizontal = 16.dp),
            style = Typography.title3Semibold,
        )
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { viewModel.updateFavorites(currentState?.toMap(), onFinish = { onClose() }) }
        ) {
            Text(
                text = stringResource(R.string.done),
                modifier =
                    Modifier.background(
                            colorResource(R.color.key),
                            RoundedCornerShape(percent = 100),
                        )
                        .padding(17.dp, 5.dp),
                color = colorResource(R.color.fill2),
                style = Typography.callout,
            )
        }
    }
}

@Composable
private fun ColumnScope.EditFavoritesList(
    routeCardData: List<RouteCardData>?,
    showStationAccessibility: Boolean,
    global: GlobalResponse?,
    deleteFavorite: (RouteStopDirection) -> Unit,
) {
    if (routeCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            LazyColumn(
                contentPadding =
                    PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(5) { LoadingRouteCard() }
            }
        }
    } else if (routeCardData.isEmpty()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            NoFavoritesView({}, false)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(routeCardData, key = { it.id }) {
                RouteCardContainer(
                    modifier = Modifier.animateItem(),
                    data = it,
                    isFavorite = { _ -> true },
                    onPin = {},
                    showStopHeader = true,
                    showStationAccessibility = showStationAccessibility,
                    enhancedFavorites = true,
                ) { stopData ->
                    FavoriteDepartures(stopData, global) {
                        val favToDelete =
                            RouteStopDirection(it.lineOrRoute.id, it.stop.id, it.directionId)
                        deleteFavorite(favToDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteDepartures(
    stopData: RouteCardData.RouteStopData,
    globalData: GlobalResponse?,
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    Column {
        stopData.data.withIndex().forEach { (index, leaf) ->
            val formatted = leaf.format(Clock.System.now(), globalData)
            val direction = stopData.directions.first { it.id == leaf.directionId }

            Row(
                modifier =
                    Modifier.padding(vertical = 10.dp, horizontal = 16.dp).clickable {
                        onClick(leaf)
                    }
            ) {
                when (formatted) {
                    is LeafFormat.Single -> {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DirectionLabel(
                                    direction,
                                    pillDecoration =
                                        formatted.route?.let {
                                            PillDecoration.OnDirectionDestination(it)
                                        },
                                )
                                Spacer(Modifier.weight(1f))
                                DeleteIcon()
                            }
                        }
                    }

                    is LeafFormat.Branched -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                            ) {
                                DirectionLabel(direction, showDestination = false)
                                BranchRows(formatted)
                            }
                            Spacer(Modifier.weight(1f))
                            DeleteIcon()
                        }
                    }
                }
            }

            if (index < stopData.data.lastIndex) {
                HaloSeparator()
            }
        }
    }
}

@Composable
private fun BranchRows(formatted: LeafFormat.Branched) {
    for (branch in formatted.branchRows) {
        val pillDecoration = branch.route?.let { PillDecoration.OnRow(it) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pillDecoration is PillDecoration.OnRow) {
                RoutePill(
                    pillDecoration.route,
                    line = null,
                    RoutePillType.Flex,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                branch.headsign,
                style = Typography.bodySemibold,
                modifier = Modifier.placeholderIfLoading(),
            )
        }
    }
}

@Composable
private fun DeleteIcon() {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(colorResource(R.color.fill2)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.trash_can),
            stringResource(R.string.delete),
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.error),
        )
    }
}
