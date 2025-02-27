package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.mbta.tid.mbta_app.android.R

@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    navigateToNearby: () -> Unit,
    navigateToMore: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier.height(83.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        actions = {
            BottomNavIconButton(
                modifier = Modifier.fillMaxSize().weight(1f),
                onClick = navigateToNearby,
                icon = R.drawable.map_pin,
                label = stringResource(R.string.nearby_transit_link),
                // currentDestination?.hiearchy?.any { it.hasRoute(Routes.NearbyTransit::class)
                // Doesn't work due to dependency issue
                // https://issuetracker.google.com/issues/360354551
                active = currentDestination?.route?.contains("NearbyTransit") ?: true
            )

            BottomNavIconButton(
                modifier = Modifier.fillMaxSize().weight(1f),
                onClick = navigateToMore,
                icon = R.drawable.more,
                label = stringResource(R.string.more_link),
                active = currentDestination?.route?.contains("More") ?: true
            )
        }
    )
}
