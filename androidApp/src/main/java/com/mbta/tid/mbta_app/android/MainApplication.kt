package com.mbta.tid.mbta_app.android

import android.app.Application
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.android.phoenix.wrapped
import com.mbta.tid.mbta_app.android.repositories.AppCheckRepository
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.initKoin
import com.mbta.tid.mbta_app.repositories.AccessibilityStatusRepository
import org.phoenixframework.Socket

val appVariant = AppVariant.Prod

class MainApplication : Application() {
    private val socket = Socket(appVariant.socketUrl, decode = ::decodeMessage)
    private val appCheck = AppCheckRepository()

    override fun onCreate() {
        super.onCreate()
        initKoin(
            appVariant,
            makeNativeModule(
                AccessibilityStatusRepository(applicationContext),
                appCheck,
                socket.wrapped()
            ),
            this
        )
    }
}
