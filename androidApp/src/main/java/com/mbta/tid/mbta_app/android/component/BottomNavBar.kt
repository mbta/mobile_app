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
import com.mbta.tid.mbta_app.android.util.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    currentDestination: Routes,
    navigateToNearby: () -> Unit,
    navigateToMore: () -> Unit
) {

    val selectedTabIndex = if (currentDestination == Routes.NearbyTransit) 0 else 1

    CompositionLocalProvider(
        LocalDensity provides
            Density(
                LocalDensity.current.density,
                // Override the system font scale so that the tab size doesn't scale up at larger
                // display sizes
                1f
            )
    ) {
        TabRow(selectedTabIndex = selectedTabIndex, indicator = {}) {
            BottomNavTab(
                selected = currentDestination == Routes.NearbyTransit,
                onClick = { navigateToNearby() },
                icon = painterResource(R.drawable.map_pin),
                text = stringResource(R.string.nearby_transit_link)
            )

            BottomNavTab(
                selected = currentDestination == Routes.More,
                onClick = { navigateToMore() },
                icon = painterResource(R.drawable.more),
                text = stringResource(R.string.more_link)
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
        modifier = Modifier.background(colorResource(R.color.fill2))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(
                text,
                style =
                    Typography.caption2.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
            )
        }
    }
}
