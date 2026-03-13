package com.mbta.tid.mbta_app.android

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.analytics.AnalyticsProvider
import com.mbta.tid.mbta_app.android.map.IMapboxConfigManager
import com.mbta.tid.mbta_app.android.map.MapboxConfigManager
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.phoenix.wrapped
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.android.widget.WidgetUpdateWorker
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.initKoin
import com.mbta.tid.mbta_app.network.NetworkConnectivityMonitor
import com.mbta.tid.mbta_app.repositories.AccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.CurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
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
                NetworkConnectivityMonitor(applicationContext),
                socket.wrapped(),
            ) + koinViewModelModule(),
            this,
        )
        scheduleWidgetUpdates()
        prewarmGlobalData()
    }

    private fun prewarmGlobalData() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { GlobalContext.get().get<IGlobalRepository>().getGlobalData() }
        }
    }

    private fun scheduleWidgetUpdates() {
        val workRequest =
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                WidgetUpdateWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )
    }

    companion object {
        fun koinViewModelModule() = module {
            single<IMapboxConfigManager> { MapboxConfigManager(get()) }
            single { SettingsCache(get()) }
            viewModelOf(::ContentViewModel)
            viewModelOf(::NearbyTransitViewModel)
        }
    }
}
