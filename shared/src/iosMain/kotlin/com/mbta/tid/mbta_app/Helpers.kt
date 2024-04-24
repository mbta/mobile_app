package com.mbta.tid.mbta_app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal fun createDataStore(): DataStore<Preferences> =
    getDataStore(
        producePath = {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/$dataStoreFileName"
        }
    )

fun initKoin() {
    startKoin { modules(appModule() + platformModule()) }
}

@DefaultArgumentInterop.Enabled
fun loadKoinMocks(
    scheduleRepository: ISchedulesRepository = IdleScheduleRepository(),
    pinnedRoutesRepository: IPinnedRoutesRepository = PinnedRoutesRepository()
) {
    loadKoinModules(
        module {
            single<ISchedulesRepository> { MockScheduleRepository() }
            single<IPinnedRoutesRepository> { PinnedRoutesRepository() }
            single { TogglePinnedRouteUsecase(get()) }
        }
    )
}

/*
Load the default Koin mock repositories and use cases, overriding their existing definitions
 */
fun loadDefaultRepoModules() {
    loadKoinModules(
        module {
            single<ISchedulesRepository> { IdleScheduleRepository() }
            single<IPinnedRoutesRepository> { PinnedRoutesRepository() }
            single { TogglePinnedRouteUsecase(get()) }
        }
    )
}

/*
Start koin with default mocks for all dependencies.
Useful for IOS testing where we want to start koin once and
subsequently load in specific modules to override these base definitions.
 */
fun startKoinIOSTestApp() {
    startKoin { modules(platformModule()) }
    loadDefaultRepoModules()
}
