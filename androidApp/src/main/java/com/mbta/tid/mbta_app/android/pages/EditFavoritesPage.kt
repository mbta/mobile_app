package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorLazyColumn
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.LoadingRouteCard
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardContainer
import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.android.favorites.NoFavoritesView
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Clock

@Composable
fun EditFavoritesPage(
    global: GlobalResponse?,
    targetLocation: Position?,
    favoritesViewModel: FavoritesViewModel,
    onClose: () -> Unit,
) {
    val initialState = favoritesViewModel.favorites.orEmpty()
    val favoritesState = remember {
        mutableStateMapOf(*initialState.map { it to true }.toTypedArray())
    }
    var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        // Don't update when global/target location changes to prevent reorders while editing
        routeCardData = favoritesViewModel.loadStaticRouteCardData(global, targetLocation)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(
            title = stringResource(R.string.edit_favorites),
            closeText = stringResource(R.string.done),
            buttonColors = ButtonDefaults.key(),
            onClose = onClose,
        )
        EditFavoritesList(routeCardData, global) {
            favoritesViewModel.updateFavorites(mapOf(it to false))
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
private fun EditFavoritesList(
    routeCardData: List<RouteCardData>?,
    global: GlobalResponse?,
    deleteFavorite: (RouteStopDirection) -> Unit,
) {
    if (routeCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            ScrollSeparatorLazyColumn(
                contentPadding =
                    PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(5) { LoadingRouteCard() }
            }
        }
    } else if (routeCardData.isEmpty()) {
        ScrollSeparatorColumn(Modifier.padding(8.dp), Arrangement.Center) {
            NoFavoritesView({}, false)
        }
    } else {
        ScrollSeparatorLazyColumn(
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

            Row(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)) {
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
                                DeleteIcon(action = { onClick(leaf) })
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
                            DeleteIcon(action = { onClick(leaf) })
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
private fun DeleteIcon(action: () -> Unit) {
    Box(
        modifier =
            Modifier.size(44.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.fill2))
                .clickable { action() }
                .testTag("trashCan"),
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
