package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import org.koin.compose.koinInject

data class ManagedFavorites(
    val favoriteRoutes: Map<RouteStopDirection, FavoriteSettings>?,
    val updateFavorites:
        suspend (Map<RouteStopDirection, FavoriteSettings?>, EditFavoritesContext, Int) -> Unit,
)

@Composable
fun manageFavorites(favoritesUseCases: FavoritesUsecases = koinInject()): ManagedFavorites {
    val favorites: Map<RouteStopDirection, FavoriteSettings>? by
        favoritesUseCases.state.collectAsState()

    LaunchedEffect(Unit) { favoritesUseCases.getRouteStopDirectionFavorites() }
    val includeAccessibility = SettingsCache.get(Settings.StationAccessibility)
    val notificationsEnabled = SettingsCache.get(Settings.Notifications)
    val updateFavorites:
        suspend (Map<RouteStopDirection, FavoriteSettings?>, EditFavoritesContext, Int) -> Unit =
        { newValues, context, defaultDirection ->
            favoritesUseCases.updateRouteStopDirections(
                newValues,
                context,
                defaultDirection,
                if (notificationsEnabled) fcmToken else null,
                includeAccessibility,
            )
        }

    return ManagedFavorites(favorites, updateFavorites)
}
