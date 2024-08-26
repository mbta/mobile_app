package com.mbta.tid.mbta_app

import IRepositories
import MockRepositories
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
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

fun initKoin(appVariant: AppVariant, nativeModule: Module) {
    startKoin { modules(appModule(appVariant) + platformModule() + nativeModule) }
}

/*
Load the Koin mock repositories and use cases, overriding their existing definitions
 */
fun loadKoinMocks(repositories: IRepositories) {
    loadKoinModules(listOf(repositoriesModule(repositories)))
}

/*
Load the default Koin mock repositories and use cases, overriding their existing definitions
 */
fun loadDefaultRepoModules() {
    loadKoinModules(repositoriesModule(MockRepositories.buildWithDefaults()))
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

fun startKoinE2E(appVariant: AppVariant, nativeModule: Module) {
    startKoin { modules(appModule(appVariant) + platformModule() + nativeModule) }
}
