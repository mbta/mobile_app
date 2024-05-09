package com.mbta.tid.mbta_app.dependencyInjection

import IRepositories
import RealRepositories
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripSchedulesRepository
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
        single<IPinnedRoutesRepository> { repositories.pinnedRoutesRepository }
        single<ISchedulesRepository> { repositories.schedules }
        single<ISettingsRepository> { repositories.settings }
        single<IStopRepository> { repositories.stop }
        single<ITripSchedulesRepository> { repositories.tripSchedules }

        single { TogglePinnedRouteUsecase(get()) }
    }
}
