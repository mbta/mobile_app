package com.mbta.tid.mbta_app

import io.ktor.client.engine.java.Java

internal actual fun getPlatform() =
    object : Platform {
        override val name = "Java ${Runtime.version()} JVM"
        override val httpClientEngine = Java.create()
        override val type: PlatformType = PlatformType.JVM
    }
