package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object JsPlatform : Platform {
    override val name: String = "JS"

    override val httpClientEngine: HttpClientEngine
        get() = Js.create()
}

internal actual fun getPlatform(): Platform = JsPlatform

@OptIn(ExperimentalUuidApi::class) internal actual fun uuid() = Uuid.random().toHexDashString()
