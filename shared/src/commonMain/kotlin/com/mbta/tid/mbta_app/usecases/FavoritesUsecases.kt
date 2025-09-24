package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import org.koin.core.component.KoinComponent

public class FavoritesUsecases(
    private val repository: IFavoritesRepository,
    private val analytics: Analytics,
) : KoinComponent {

    public suspend fun getRouteStopDirectionFavorites(): Map<RouteStopDirection, FavoriteSettings> {
        val storedFavorites = repository.getFavorites()
        return storedFavorites.routeStopDirection
    }

    public suspend fun updateRouteStopDirections(
        newValues: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = storedFavorites.routeStopDirection.toMutableMap()

        val changedFavorites =
            newValues
                .filter {
                    (it.value == null && currentFavorites.containsKey(it.key)) ||
                        (it.value != null && !(currentFavorites.containsKey(it.key)))
                }
                .mapValues { it.value != null }

        analytics.favoritesUpdated(changedFavorites, context, defaultDirection)

        newValues.forEach { (routeStopDirection, settings) ->
            if (settings != null) {
                currentFavorites.put(routeStopDirection, settings)
            } else {
                currentFavorites.remove(routeStopDirection)
            }
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
    }
}

public enum class EditFavoritesContext {
    Favorites,
    StopDetails,
    RouteDetails,
}
