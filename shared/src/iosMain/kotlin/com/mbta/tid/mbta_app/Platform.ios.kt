package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice

internal class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val httpClientEngine: HttpClientEngine
        get() = Darwin.create()
}

internal actual fun getPlatform(): Platform = IOSPlatform()

internal actual fun uuid(): String = NSUUID().UUIDString()
