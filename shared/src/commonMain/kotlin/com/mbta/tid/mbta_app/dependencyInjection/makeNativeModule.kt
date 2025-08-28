package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.AlertsRepository
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.ICurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.PredictionsRepository
import com.mbta.tid.mbta_app.repositories.TripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.VehicleRepository
import com.mbta.tid.mbta_app.repositories.VehiclesRepository
import kotlinx.coroutines.channels.BufferOverflow
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

public fun makeNativeModule(
    accessibilityStatus: IAccessibilityStatusRepository,
    analytics: Analytics,
    currentAppVersion: ICurrentAppVersionRepository,
    socket: PhoenixSocket,
    onEventBufferOverflow: BufferOverflow,
): Module {
    return module {
        single<IAccessibilityStatusRepository> { accessibilityStatus }
        single<Analytics> { analytics }
        single<ICurrentAppVersionRepository> { currentAppVersion }
        single<PhoenixSocket> { socket }
        single<BufferOverflow>(named(KoinName.OnEventBufferOverflow)) { onEventBufferOverflow }
        factory<IAlertsRepository> { AlertsRepository(get()) }
        factory<IPredictionsRepository> { PredictionsRepository(get()) }
        factory<ITripPredictionsRepository> { TripPredictionsRepository(get()) }
        factory<IVehicleRepository> { VehicleRepository(get()) }
        factory<IVehiclesRepository> { VehiclesRepository(get()) }
    }
}
