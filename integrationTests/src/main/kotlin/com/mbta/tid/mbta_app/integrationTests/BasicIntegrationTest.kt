package com.mbta.tid.mbta_app.integrationTests

import io.appium.java_client.AppiumDriver

fun runBasicIntegrationTest(driver: AppiumDriver, platformAppElements: PlatformAppElements) {
    val el = driver.findElement(platformAppElements.sheetHandle)
    el.click()
    println(driver.pageSource)
}
