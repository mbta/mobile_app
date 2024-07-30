package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AlertTest {
    @Test
    fun `alert status is set properly`() {
        val objects = ObjectCollectionBuilder()
        val closureAlert = objects.alert { effect = Alert.Effect.StationClosure }
        val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
        val detourAlert = objects.alert { effect = Alert.Effect.Detour }
        val issueAlert = objects.alert { effect = Alert.Effect.ParkingIssue }

        assertEquals(StopAlertState.Suspension, closureAlert.alertState)
        assertEquals(StopAlertState.Shuttle, shuttleAlert.alertState)
        assertEquals(StopAlertState.Issue, detourAlert.alertState)
        assertEquals(StopAlertState.Issue, issueAlert.alertState)
    }
}
