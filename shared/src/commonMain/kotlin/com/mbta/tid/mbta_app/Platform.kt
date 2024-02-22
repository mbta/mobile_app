package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine

interface Platform {
    val name: String

    val httpClientEngine: HttpClientEngine
}

expect fun getPlatform(): Platform

expect fun uuid(): String
