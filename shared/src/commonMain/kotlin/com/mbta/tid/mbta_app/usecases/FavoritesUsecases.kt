package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import org.koin.core.component.KoinComponent

class FavoritesUsecases(
    private val repository: IFavoritesRepository,
    private val analytics: Analytics,
) : KoinComponent {

    suspend fun getRouteStopDirectionFavorites(): Set<RouteStopDirection> {
        val storedFavorites = repository.getFavorites()
        return storedFavorites.routeStopDirection ?: emptySet()
    }

    suspend fun updateRouteStopDirections(
        newValues: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = (storedFavorites.routeStopDirection ?: emptySet()).toMutableSet()

        val changedFavorites =
            newValues.filter {
                (!it.value && currentFavorites.contains(it.key)) ||
                    (it.value && !(currentFavorites.contains(it.key)))
            }

        analytics.favoritesUpdated(changedFavorites, context, defaultDirection)

        newValues.forEach { (routeStopDirection, isFavorite) ->
            if (isFavorite) {
                currentFavorites.add(routeStopDirection)
            } else {
                currentFavorites.remove(routeStopDirection)
            }
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
    }
}

enum class EditFavoritesContext {
    Favorites,
    StopDetails,
}
