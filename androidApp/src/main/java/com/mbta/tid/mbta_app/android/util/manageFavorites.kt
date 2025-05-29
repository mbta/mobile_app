package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import org.koin.compose.koinInject

data class ManagedFavorites(
    val favoriteRoutes: Set<RouteStopDirection>?,
    val toggleFavorite: suspend (RouteStopDirection) -> Boolean,
)

@Composable
fun manageFavorites(favoritesUseCases: FavoritesUsecases = koinInject()): ManagedFavorites {
    var favorites: Set<RouteStopDirection>? by remember { mutableStateOf(null) }

    LaunchedEffect(null) { favorites = favoritesUseCases.getRouteStopDirectionFavorites() }

    val toggleFavorite: suspend (RouteStopDirection) -> Boolean = { rsd ->
        val isFavorite = favoritesUseCases.toggleRouteStopDirection(rsd)
        favorites = favoritesUseCases.getRouteStopDirectionFavorites()
        isFavorite
    }

    return ManagedFavorites(favorites, toggleFavorite)
}
