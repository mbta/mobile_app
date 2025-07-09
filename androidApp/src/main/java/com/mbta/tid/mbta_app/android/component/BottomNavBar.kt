package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.Routes
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.repositories.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    currentDestination: Routes,
    sheetNavEntrypoint: SheetRoutes.Entrypoint,
    navigateToFavorites: () -> Unit,
    navigateToNearby: () -> Unit,
    navigateToMore: () -> Unit,
) {
    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)

    val selectedTabIndex =
        if (enhancedFavorites) {
            when (currentDestination) {
                is Routes.MapAndSheet ->
                    when (sheetNavEntrypoint) {
                        SheetRoutes.Favorites -> 0
                        SheetRoutes.NearbyTransit -> 1
                    }
                is Routes.More -> 2
            }
        } else {
            when (currentDestination) {
                is Routes.MapAndSheet -> 0
                is Routes.More -> 1
            }
        }

    CompositionLocalProvider(
        LocalDensity provides
            Density(
                LocalDensity.current.density,
                // Override the system font scale so that the tab size doesn't scale up at larger
                // display sizes
                // https://stackoverflow.com/a/74290870
                1f,
            )
    ) {
        TabRow(selectedTabIndex = selectedTabIndex, indicator = {}) {
            if (enhancedFavorites) {
                BottomNavTab(
                    selected =
                        currentDestination is Routes.MapAndSheet &&
                            sheetNavEntrypoint == SheetRoutes.Favorites,
                    onClick = { navigateToFavorites() },
                    icon = painterResource(R.drawable.tab_star),
                    text = stringResource(R.string.favorites_link),
                )
            }

            BottomNavTab(
                selected =
                    currentDestination is Routes.MapAndSheet &&
                        sheetNavEntrypoint == SheetRoutes.NearbyTransit,
                onClick = { navigateToNearby() },
                icon = painterResource(R.drawable.map_pin),
                text = stringResource(R.string.nearby_transit_link),
            )

            BottomNavTab(
                selected = currentDestination == Routes.More,
                onClick = { navigateToMore() },
                icon = painterResource(R.drawable.more),
                text = stringResource(R.string.more_link),
            )
        }
    }
}

@Composable
private fun BottomNavTab(selected: Boolean, onClick: () -> Unit, icon: Painter, text: String) {
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = colorResource(R.color.key),
        unselectedContentColor = colorResource(R.color.text),
        modifier = Modifier.background(colorResource(R.color.fill2)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(4.dp))
            Text(
                text,
                style =
                    Typography.caption2.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    ),
            )
        }
    }
}
