package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Facility
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AlertsStreamDataResponseTest {
    @Test
    fun `provided global facilities data is injected into alerts`() {
        val objects = ObjectCollectionBuilder()
        val facility = objects.facility { type = Facility.Type.Elevator }
        val alert =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
            }

        val alertData = AlertsStreamDataResponse(objects)
        assertEquals(alert, alertData.getAlert(alert.id))

        val injectedData = alertData.injectFacilities(GlobalResponse(objects))
        assertNotEquals(alertData, injectedData)
        assertNotEquals(alert, injectedData.getAlert(alert.id))
        assertEquals(facility, injectedData.getAlert(alert.id)?.facilities?.get(facility.id))
    }

    @Test
    fun `alert data is passed through unchanged if global data is null`() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {}

        val alertData = AlertsStreamDataResponse(objects)
        assertEquals(alert, alertData.getAlert(alert.id))

        val injectedData = alertData.injectFacilities(null)
        assertEquals(alertData, injectedData)
    }

    @Test
    fun `isNullOrEmpty is true when null or empty`() {
        val nullResponse: AlertsStreamDataResponse? = null
        assertTrue(nullResponse.isNullOrEmpty())

        val emptyResponse = AlertsStreamDataResponse(emptyMap())
        assertTrue(emptyResponse.isNullOrEmpty())
    }

    @Test
    fun `isNullOrEmpty is false when alerts exist`() {
        val objects = ObjectCollectionBuilder()
        objects.alert {}
        val alertData = AlertsStreamDataResponse(objects)
        assertFalse(alertData.isNullOrEmpty())
    }
}
