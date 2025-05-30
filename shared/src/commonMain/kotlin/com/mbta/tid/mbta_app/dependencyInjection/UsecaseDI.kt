package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.FeaturePromoUseCase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UsecaseDI : KoinComponent {
    val alertsUsecase: AlertsUsecase by inject()
    val configUsecase: ConfigUseCase by inject()
    val featurePromoUsecase: FeaturePromoUseCase by inject()
    val toggledPinnedRouteUsecase: TogglePinnedRouteUsecase by inject()
    val visitHistoryUsecase: VisitHistoryUsecase by inject()
}
