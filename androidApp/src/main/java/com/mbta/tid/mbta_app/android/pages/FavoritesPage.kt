package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.favorites.FavoritesView
import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.model.SheetRoutes
import io.github.dellisd.spatialk.geojson.Position

@Composable
fun FavoritesPage(
    openSheetRoute: (SheetRoutes) -> Unit,
    favoritesViewModel: FavoritesViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    nearbyTransit: NearbyTransit,
) {
    var targetLocation by remember { mutableStateOf<Position?>(null) }
    managedTargetLocation(
        nearbyTransit = nearbyTransit,
        updateTargetLocation = { targetLocation = it },
        reset = { /* no-op */ },
    )

    FavoritesView(
        openSheetRoute,
        favoritesViewModel,
        errorBannerViewModel,
        nearbyTransit.alertData,
        nearbyTransit.globalResponse,
        targetLocation,
    )
}
