package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    override val httpClientEngine: HttpClientEngine
        get() = Android.create()
}

actual fun getPlatform(): Platform = AndroidPlatform()
