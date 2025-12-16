@file:OptIn(ExperimentalObjCName::class)
@file:Suppress("unused")

package com.mbta.tid.mbta_app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.IRepositories
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.endToEnd.endToEndModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.experimental.ExperimentalObjCName
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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
import platform.Foundation.dateWithTimeIntervalSince1970

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

public fun initKoin(appVariant: AppVariant, nativeModule: Module) {
    startKoin {
        modules(appModule(appVariant) + viewModelModule() + platformModule() + nativeModule)
    }
}

/*
Load the Koin mock repositories and use cases, overriding their existing definitions
 */
public fun loadKoinMocks(repositories: IRepositories) {
    loadKoinModules(listOf(repositoriesModule(repositories)))
}

/*
Load the Koin mock repositories using the provided objects
 */
public fun loadKoinMocks(objects: ObjectCollectionBuilder) {
    val repositories = MockRepositories()
    repositories.useObjects(objects)
    loadKoinModules(listOf(repositoriesModule(repositories)))
}

/*
Load the default Koin mock repositories and use cases, overriding their existing definitions
 */
public fun loadDefaultRepoModules() {
    loadKoinModules(repositoriesModule(MockRepositories()))
}

/*
Start koin with default mocks for all dependencies.
Useful for IOS testing where we want to start koin once and
subsequently load in specific modules to override these base definitions.
 */
public fun startKoinIOSTestApp() {
    startKoin {
        modules(
            platformModule() +
                viewModelModule() +
                module {
                    single<Analytics> { MockAnalytics() }
                    single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                        Dispatchers.Default
                    }
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) { Dispatchers.IO }
                }
        )
    }
    loadDefaultRepoModules()
}

public fun startKoinE2E() {
    startKoin {
        modules(
            endToEndModule() + viewModelModule() + module { single<Analytics> { MockAnalytics() } }
        )
    }
}

// rather than having 2^n variants of copy via SKIE, define the exact one we need
public fun Vehicle.butWith(bearing: Double): Vehicle = copy(bearing = bearing)

/** Converts an [NSDate] into an [EasternTimeInstant]. */
public fun NSDate.toEasternInstant(): EasternTimeInstant =
    EasternTimeInstant(this.toKotlinInstant())

/** Converts an [EasternTimeInstant] into an [NSDate]. Loses time zone information. */
public fun EasternTimeInstant.toNSDateLosingTimeZone(): NSDate =
    NSDate.dateWithTimeIntervalSince1970(this.toEpochFracSeconds())

@ObjCName("plus")
public fun EasternTimeInstant.plusSeconds(seconds: Int): EasternTimeInstant = this + seconds.seconds

@ObjCName("minus")
public fun EasternTimeInstant.minusSeconds(seconds: Int): EasternTimeInstant =
    this - seconds.seconds

@ObjCName("plus")
public fun EasternTimeInstant.plusMinutes(minutes: Int): EasternTimeInstant = this + minutes.minutes

@ObjCName("minus")
public fun EasternTimeInstant.minusMinutes(minutes: Int): EasternTimeInstant =
    this - minutes.minutes

@ObjCName("plus")
public fun EasternTimeInstant.plusHours(hours: Int): EasternTimeInstant = this + hours.hours

@ObjCName("minus")
public fun EasternTimeInstant.minusHours(hours: Int): EasternTimeInstant = this - hours.hours

public fun LocalDate.plus(days: Int): LocalDate = this.plus(DatePeriod(days = days))

public val SystemClock: Clock = Clock.System
