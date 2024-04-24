package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule(appVariant: AppVariant) = module {
    single { MobileBackendClient(appVariant) }
    single<ISchedulesRepository> { SchedulesRepository() }
    single<IPinnedRoutesRepository> { PinnedRoutesRepository() }
    single { TogglePinnedRouteUsecase(get()) }
}
