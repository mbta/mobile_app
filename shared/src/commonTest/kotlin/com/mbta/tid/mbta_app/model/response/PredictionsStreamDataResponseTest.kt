package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class PredictionsStreamDataResponseTest {
    @Test
    fun `predictionQuantity counts predictions`() {
        val objects = ObjectCollectionBuilder()
        val p1 = objects.prediction()
        val p2 = objects.prediction()
        val p3 = objects.prediction()

        val data =
            PredictionsStreamDataResponse(
                predictions = mapOf(p1.id to p1, p2.id to p2, p3.id to p3),
                trips = emptyMap(),
                vehicles = emptyMap()
            )

        assertEquals(3, data.predictionQuantity())
    }
}
