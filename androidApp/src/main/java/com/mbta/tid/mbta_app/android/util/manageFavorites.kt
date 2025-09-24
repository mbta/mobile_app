package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
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
    var favorites: Map<RouteStopDirection, FavoriteSettings>? by remember { mutableStateOf(null) }

    LaunchedEffect(null) { favorites = favoritesUseCases.getRouteStopDirectionFavorites() }

    val updateFavorites:
        suspend (Map<RouteStopDirection, FavoriteSettings?>, EditFavoritesContext, Int) -> Unit =
        { newValues, context, defaultDirection ->
            favoritesUseCases.updateRouteStopDirections(newValues, context, defaultDirection)
            favorites = favoritesUseCases.getRouteStopDirectionFavorites()
        }

    return ManagedFavorites(favorites, updateFavorites)
}
