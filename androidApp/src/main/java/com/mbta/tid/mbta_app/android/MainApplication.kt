package com.mbta.tid.mbta_app.android

import android.app.Application
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.android.phoenix.wrapped
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.dependencyInjection.makeNativeModule
import com.mbta.tid.mbta_app.initKoin
import org.phoenixframework.Socket

val appVariant = AppVariant.Prod

class MainApplication : Application() {
    private val socket = Socket(appVariant.socketUrl, decode = ::decodeMessage)

    override fun onCreate() {
        super.onCreate()
        initKoin(appVariant, makeNativeModule(socket.wrapped()))
    }
}