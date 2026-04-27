package com.mbta.tid.mbta_app.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TargetingTest {
    @Test
    fun testNullInstanceId() {
        assertFalse(Targeting.get(Targeting.Target.NotificationsBeta, null))
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testNotificationsBetaRatio() {
        val boolCounts = mutableMapOf(true to 0, false to 0)
        val totalTries = 10000
        for (i in 0..totalTries) {
            val uuid = Uuid.generateV4()
            val targetResult = Targeting.get(Targeting.Target.NotificationsBeta, uuid.toHexString())
            boolCounts[targetResult] = boolCounts[targetResult]?.plus(1) ?: 0
        }
        val ratio = (boolCounts[true]?.toFloat() ?: 0f) / totalTries
        assertTrue(ratio in 0.04..0.06)
    }
}
