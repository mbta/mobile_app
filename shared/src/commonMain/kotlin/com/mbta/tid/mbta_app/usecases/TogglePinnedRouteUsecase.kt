package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TogglePinnedRouteUsecase : KoinComponent {

    private val repository: IPinnedRoutesRepository by inject()

    suspend fun execute(route: String) {
        val currentRoutes = repository.getPinnedRoutes().toMutableSet()
        if (currentRoutes.contains(route)) {
            currentRoutes.remove(route)
        } else {
            currentRoutes.add(route)
        }
        repository.setPinnedRoutes(currentRoutes)
    }
}
