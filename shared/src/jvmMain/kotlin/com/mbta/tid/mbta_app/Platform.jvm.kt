package com.mbta.tid.mbta_app

import io.ktor.client.engine.java.Java
import java.util.UUID

internal actual fun getPlatform() =
    object : Platform {
        override val name = "Java ${Runtime.version()} JVM"
        override val httpClientEngine = Java.create()
        override val type: PlatformType = PlatformType.JVM
    }

internal actual fun uuid() = UUID.randomUUID().toString()
