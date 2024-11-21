package com.mbta.tid.mbta_app.android

import android.app.Application
import com.mbta.tid.mbta_app.android.phoenix.wrapped
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.initKoin
import com.mbta.tid.mbta_app.repositories.AccessibilityStatusRepository
import org.phoenixframework.Socket

// unfortunately, expect/actual only works in multiplatform projects, so we can't
// expect val appVariant: AppVariant

class MainApplication : Application() {
    private val socket = Socket(appVariant.socketUrl, decode = ::decodeMessage)

    override fun onCreate() {
        super.onCreate()
        initKoin(
            appVariant,
            makeNativeModule(AccessibilityStatusRepository(applicationContext), socket.wrapped()),
            this
        )
    }
}
