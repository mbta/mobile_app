package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import kotlin.test.Test
import kotlin.test.assertEquals

class TripTest {
    @Test
    fun `routePatternId can be null`() {

        assertEquals(
            Trip(
                id = "1",
                directionId = 0,
                headsign = "Harvard",
                routePatternId = null,
                shapeId = null
            ),
            json.decodeFromString<Trip>(
                """
                {"id": "1",
                "direction_id":0,
                "headsign": "Harvard",
                "routePatternId":null,
                "shapeId":null}
            """
                    .trimIndent()
            )
        )
    }
}
