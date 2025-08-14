package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import org.koin.core.component.KoinComponent

public class TogglePinnedRouteUsecase(private val repository: IPinnedRoutesRepository) :
    KoinComponent {

    // Boolean return value indicates pinned or unpinned state
    public suspend fun execute(route: String): Boolean {
        val currentRoutes = repository.getPinnedRoutes().toMutableSet()
        val containsRoute = currentRoutes.contains(route)
        if (containsRoute) {
            currentRoutes.remove(route)
        } else {
            currentRoutes.add(route)
        }
        repository.setPinnedRoutes(currentRoutes)
        return !containsRoute
    }
}
