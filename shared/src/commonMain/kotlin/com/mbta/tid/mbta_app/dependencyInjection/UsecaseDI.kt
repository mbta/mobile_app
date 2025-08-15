package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.usecases.IFeaturePromoUseCase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public class UsecaseDI : KoinComponent {
    public val alertsUsecase: AlertsUsecase by inject()
    public val configUsecase: ConfigUseCase by inject()
    public val featurePromoUsecase: IFeaturePromoUseCase by inject()
    public val visitHistoryUsecase: VisitHistoryUsecase by inject()
    public val favoritesUsecases: FavoritesUsecases by inject()
}
