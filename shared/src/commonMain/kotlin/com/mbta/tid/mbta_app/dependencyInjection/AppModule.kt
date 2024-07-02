package com.mbta.tid.mbta_app.dependencyInjection

import IRepositories
import RealRepositories
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule(appVariant: AppVariant) = module {
    includes(
        module { single { MobileBackendClient(appVariant) } },
        repositoriesModule(RealRepositories())
    )
}

fun repositoriesModule(repositories: IRepositories): Module {
    return module {
        single<IPinnedRoutesRepository> { repositories.pinnedRoutes }
        single<ISchedulesRepository> { repositories.schedules }
        single<ISettingsRepository> { repositories.settings }
        single<IStopRepository> { repositories.stop }
        single<ITripRepository> { repositories.tripSchedules }
        repositories.predictions?.let { predictionsRepo ->
            factory<IPredictionsRepository> { predictionsRepo }
        }
        repositories.alerts?.let { alertsRepo -> factory<IAlertsRepository> { alertsRepo } }
        single<INearbyRepository> { repositories.nearby }
        repositories.tripPredictions?.let { tripPredictionsRepo ->
            factory<ITripPredictionsRepository> { tripPredictionsRepo }
        }
        repositories.vehicle?.let { vehicleRepo -> factory<IVehicleRepository> { vehicleRepo } }
        single { TogglePinnedRouteUsecase(get()) }
    }
}
