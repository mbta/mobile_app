package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    override val httpClientEngine: HttpClientEngine
        get() = OkHttp.create()
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun uuid(): String = UUID.randomUUID().toString()
