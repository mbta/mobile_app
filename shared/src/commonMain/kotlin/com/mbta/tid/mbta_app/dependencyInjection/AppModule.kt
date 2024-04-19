package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule() = module {
    single { MobileBackendClient() }
    single<ISchedulesRepository> { SchedulesRepository() }
}
