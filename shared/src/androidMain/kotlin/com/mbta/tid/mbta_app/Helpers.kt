package com.mbta.tid.mbta_app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mbta.tid.mbta_app.dependencyInjection.appModule
import org.koin.core.context.startKoin

internal fun createDataStore(context: Context): DataStore<Preferences> =
    getDataStore(producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath })

fun initKoin(appVariant: AppVariant) {
    startKoin { modules(appModule(appVariant) + platformModule()) }
}
