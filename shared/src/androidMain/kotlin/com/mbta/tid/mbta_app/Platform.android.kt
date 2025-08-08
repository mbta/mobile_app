package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.UUID

internal class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    override val httpClientEngine: HttpClientEngine
        get() = OkHttp.create()
}

internal actual fun getPlatform(): Platform = AndroidPlatform()

internal actual fun uuid(): String = UUID.randomUUID().toString()
