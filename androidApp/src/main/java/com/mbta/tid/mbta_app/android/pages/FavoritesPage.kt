package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.favorites.FavoritesView
import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.model.SheetRoutes
import io.github.dellisd.spatialk.geojson.Position

@Composable
fun FavoritesPage(
    openSheetRoute: (SheetRoutes) -> Unit,
    targetLocation: Position?,
    favoritesViewModel: FavoritesViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    nearbyTransit: NearbyTransit,
) {
    FavoritesView(
        openSheetRoute,
        favoritesViewModel,
        errorBannerViewModel,
        nearbyTransit.alertData,
        nearbyTransit.globalResponse,
        targetLocation,
    )
}
