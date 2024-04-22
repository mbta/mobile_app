package com.mbta.tid.mbta_app

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val httpClientEngine: HttpClientEngine
        get() = Darwin.create()
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun uuid(): String = NSUUID().UUIDString()
