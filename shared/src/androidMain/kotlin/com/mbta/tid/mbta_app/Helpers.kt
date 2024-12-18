package com.mbta.tid.mbta_app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

internal fun createDataStore(context: Context): DataStore<Preferences> =
    getDataStore(producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath })

fun initKoin(
    appVariant: AppVariant,
    nativeModule: Module,
    viewModelModule: Module,
    context: Context
) {
    startKoin {
        androidContext(context)
        modules(appModule(appVariant) + platformModule() + nativeModule + viewModelModule)
    }
}
