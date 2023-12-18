package com.mbta.tid.mbta_app

import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidGreetingTest {

    @Test
    fun testExample() {
        // TODO: Switch back to assertTrue. See how test failure is represented in CI
        assertTrue("Check Android is mentioned", Greeting().greet().contains("Android"))
    }
}
