@file:JvmName("AndroidHost")

package com.mbta.tid.mbta_app.integrationTests

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.openqa.selenium.By

object AndroidApp : PlatformAppElements {
    override val sheetHandle: By = AppiumBy.accessibilityId("Drag handle")
}

fun main(args: Array<String>) = runInAppium {
    val apkPath = Path(args[0])

    check(apkPath.exists())

    val options: UiAutomator2Options =
        UiAutomator2Options().setAvd("Pixel_6a_API_33").setApp(apkPath.toString())
    val driver = AndroidDriver(URL("http://127.0.0.1:4723"), options)
    try {
        runBasicIntegrationTest(driver, AndroidApp)
    } finally {
        driver.quit()
    }
}
