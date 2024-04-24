package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsecaseDI() : KoinComponent {
    val toggledPinnedRouteUsecase: TogglePinnedRouteUsecase by inject()
}
