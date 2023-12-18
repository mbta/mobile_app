package com.mbta.tid.mbta_app

import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidGreetingTest {

    @Test
    fun testExample() {
        // TODO: Switch back to assertTrue. See how test failure is represented in CI
        assertFalse("Check Android is mentioned", Greeting().greet().contains("Android"))
    }
}
