package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine

internal interface Platform {
    val name: String

    val httpClientEngine: HttpClientEngine

    val type: PlatformType
}

internal expect fun getPlatform(): Platform

public enum class PlatformType {
    iOS,
    Android,
    JVM,
}
