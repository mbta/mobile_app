package com.mbta.tid.mbta_app.integrationTests

import io.appium.java_client.service.local.AppiumServiceBuilder
import java.io.File

fun runInAppium(block: () -> Unit) {
    val service = AppiumServiceBuilder().withAppiumJS(File(System.getenv("APPIUM_PATH"))).build()
    service.start()
    try {
        block()
    } finally {
        service.stop()
    }
}
