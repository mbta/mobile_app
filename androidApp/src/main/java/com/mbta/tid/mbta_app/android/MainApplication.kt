package com.mbta.tid.mbta_app.android

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.analytics.AnalyticsProvider
import com.mbta.tid.mbta_app.android.map.IMapboxConfigManager
import com.mbta.tid.mbta_app.android.map.MapboxConfigManager
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.phoenix.wrapped
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.initKoin
import com.mbta.tid.mbta_app.repositories.AccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.CurrentAppVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import org.koin.core.module.dsl.*
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.phoenixframework.Socket

// unfortunately, expect/actual only works in multiplatform projects, so we can't
// expect val appVariant: AppVariant

class MainApplication : Application() {
    private val socket = Socket(appVariant.socketUrl, decode = ::decodeMessage)

    override fun onCreate() {
        super.onCreate()
        initKoin(
            appVariant,
            makeNativeModule(
                AccessibilityStatusRepository(applicationContext),
                if (R.string::class.members.any { it.name == "google_app_id" })
                    AnalyticsProvider(Firebase.analytics)
                else MockAnalytics(),
                CurrentAppVersionRepository(BuildConfig.VERSION_NAME),
                socket.wrapped(),
                if (BuildConfig.DEBUG) BufferOverflow.SUSPEND else BufferOverflow.DROP_OLDEST,
            ) + koinViewModelModule(),
            this,
        )
    }

    companion object {
        fun koinViewModelModule() = module {
            single<IMapboxConfigManager> { MapboxConfigManager(get()) }
            single { SettingsCache(get()) }
            viewModelOf(::ContentViewModel)
            viewModelOf(::NearbyTransitViewModel)
            viewModel {
                StopDetailsViewModel(get(), get(), get(), get(), get(), get(), Dispatchers.Default)
            }
        }
    }
}
