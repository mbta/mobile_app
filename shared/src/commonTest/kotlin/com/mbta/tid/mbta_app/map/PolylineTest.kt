package com.mbta.tid.mbta_app.map

import kotlin.test.Test
import kotlin.test.assertEquals
import org.maplibre.spatialk.geojson.Position

class PolylineTest {
    @Test
    fun `decodes Google sample`() {
        val data = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        assertEquals(
            listOf(
                Position(latitude = 38.5, longitude = -120.2),
                Position(latitude = 40.7, longitude = -120.95),
                Position(latitude = 43.252, longitude = -126.453),
            ),
            Polyline.decode(data),
        )
    }

    @Test
    fun `matches other sample`() {
        val data = "qznaGt~wpLcDkb@kWmL"
        assertEquals(
            listOf(
                Position(latitude = 42.35193, longitude = -71.07067),
                Position(latitude = 42.35275, longitude = -71.06501),
                Position(latitude = 42.35665, longitude = -71.06286),
            ),
            Polyline.decode(data),
        )
    }

    @Test
    fun `matches real data`() {
        val data = "{epaGptspLCaA}@meAqDwv@KqB"
        assertApproximatelyEquals(
            listOf(
                Position(latitude = 42.35886, longitude = -71.04857),
                Position(latitude = 42.35888, longitude = -71.04824),
                Position(latitude = 42.35919, longitude = -71.03697),
                Position(latitude = 42.36008, longitude = -71.02805),
                Position(latitude = 42.36014, longitude = -71.02748),
            ),
            Polyline.decode(data),
        )
    }

    private fun assertApproximatelyEquals(expected: List<Position>, actual: List<Position>) {
        assertEquals(expected.size, actual.size, "sizes don't match")
        expected.zip(actual).mapIndexed { index, (expectedPosition, actualPosition) ->
            assertEquals(
                expectedPosition.latitude,
                actualPosition.latitude,
                1e-6,
                "latitude mismatch at index $index",
            )
            assertEquals(
                expectedPosition.longitude,
                actualPosition.longitude,
                1e-6,
                "longitude mismatch at index $index",
            )
        }
    }
}
