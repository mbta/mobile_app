package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
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
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

public fun makeNativeModule(
    accessibilityStatus: IAccessibilityStatusRepository,
    analytics: Analytics,
    currentAppVersion: ICurrentAppVersionRepository,
    networkConnectivityMonitor: INetworkConnectivityMonitor,
    socket: PhoenixSocket,
): Module {
    return module {
        single<IAccessibilityStatusRepository> { accessibilityStatus }
        single<Analytics> { analytics }
        single<ICurrentAppVersionRepository> { currentAppVersion }
        single<INetworkConnectivityMonitor> { networkConnectivityMonitor }
        single<PhoenixSocket> { socket }
        factory<IAlertsRepository> {
            AlertsRepository(get(), get(), get(named("coroutineDispatcherIO")))
        }
        factory<IPredictionsRepository> {
            PredictionsRepository(get(), get(named("coroutineDispatcherIO")))
        }
        factory<ITripPredictionsRepository> {
            TripPredictionsRepository(get(), get(named("coroutineDispatcherIO")))
        }
        factory<IVehicleRepository> {
            VehicleRepository(get(), get(named("coroutineDispatcherIO")))
        }
        factory<IVehiclesRepository> {
            VehiclesRepository(get(), get(named("coroutineDispatcherIO")))
        }
    }
}
