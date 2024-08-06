@file:JvmName("IosHost")

package com.mbta.tid.mbta_app.integrationTests

import io.appium.java_client.AppiumBy
import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.ios.options.XCUITestOptions
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.exists

object IosApp : PlatformAppElements {
    override val sheetHandle = AppiumBy.accessibilityId("Sheet Grabber")
}

fun main(args: Array<String>) = runInAppium {
    val ipaPath = Path(args[0])

    check(ipaPath.exists())

    val options =
        XCUITestOptions().setUdid("4C43072E-7EC1-41EA-8091-11311C0C7571").setApp(ipaPath.toString())
    val driver = IOSDriver(URL("http://127.0.0.1:4723"), options)
    try {
        runBasicIntegrationTest(driver, IosApp)
    } finally {
        driver.quit()
    }
}
