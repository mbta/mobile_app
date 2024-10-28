package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.AlertsRepository
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.PredictionsRepository
import com.mbta.tid.mbta_app.repositories.TripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.VehicleRepository
import com.mbta.tid.mbta_app.repositories.VehiclesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

fun makeNativeModule(
    accessibilityStatus: IAccessibilityStatusRepository,
    appCheck: IAppCheckRepository,
    socket: PhoenixSocket
): Module {
    return module {
        single<IAccessibilityStatusRepository> { accessibilityStatus }
        single<IAppCheckRepository> { appCheck }
        single<PhoenixSocket> { socket }
        factory<IAlertsRepository> { AlertsRepository(get()) }
        factory<IPredictionsRepository> { PredictionsRepository(get()) }
        factory<ITripPredictionsRepository> { TripPredictionsRepository(get()) }
        factory<IVehicleRepository> { VehicleRepository(get()) }
        factory<IVehiclesRepository> { VehiclesRepository(get()) }
    }
}
