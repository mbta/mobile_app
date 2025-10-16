package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    override val httpClientEngine: HttpClientEngine
        get() = OkHttp.create()

    override val type: PlatformType = PlatformType.Android
}

internal actual fun getPlatform(): Platform = AndroidPlatform()
