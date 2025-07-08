package com.mbta.tid.mbta_app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.CoroutineDispatcherKoinId
import com.mbta.tid.mbta_app.dependencyInjection.IRepositories
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.endToEnd.endToEndModule
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.time.Instant
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.toKotlinInstant
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDate
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
    startKoin {
        modules(appModule(appVariant) + viewModelModule() + platformModule() + nativeModule)
    }
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
    loadKoinModules(repositoriesModule(MockRepositories()))
}

/*
Start koin with default mocks for all dependencies.
Useful for IOS testing where we want to start koin once and
subsequently load in specific modules to override these base definitions.
 */
fun startKoinIOSTestApp() {
    startKoin {
        modules(
            platformModule() +
                viewModelModule() +
                module {
                    single<Analytics> { MockAnalytics() }
                    single<CoroutineDispatcher>(named(CoroutineDispatcherKoinId.Default)) {
                        Dispatchers.Default
                    }
                    single<CoroutineDispatcher>(named(CoroutineDispatcherKoinId.IO)) {
                        Dispatchers.IO
                    }
                }
        )
    }
    loadDefaultRepoModules()
}

fun startKoinE2E() {
    startKoin {
        modules(
            endToEndModule() + viewModelModule() + module { single<Analytics> { MockAnalytics() } }
        )
    }
}

/**
 * Converts an [NSDate] into a Kotlin stdlib [Instant].
 *
 * Necessary because the real [kotlinx.datetime.toKotlinInstant] has an internal-visibility-only
 * default parameter and default parameters evaporate in Objective-C interop.
 */
fun NSDate.toKotlinInstant(): Instant = this.toKotlinInstant()
