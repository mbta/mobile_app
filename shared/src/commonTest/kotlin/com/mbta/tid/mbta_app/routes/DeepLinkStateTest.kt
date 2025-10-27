package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkStateTest {
    @Test
    fun `matches root`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(DeepLinkState.None, DeepLinkState.from(variant.backendRoot))
    }

    @Test
    fun `matches unfiltered stop`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Stop("place-chill", null, null, null),
            DeepLinkState.from("${variant.backendRoot}/s/place-chill"),
        )
    }

    @Test
    fun `matches filtered stop`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Stop("place-chill", "Green-B", 1, "12345"),
            DeepLinkState.from("${variant.backendRoot}/s/place-chill/r/Green-B/d/1/t/12345"),
        )
    }

    @Test
    fun `stop has a corresponding SheetRoutes`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        val deepLink =
            DeepLinkState.from("${variant.backendRoot}/s/place-chill/r/Green-B/d/1/t/12345")
                as? DeepLinkState.Stop
        assertEquals(
            SheetRoutes.StopDetails(
                "place-chill",
                StopDetailsFilter(LineOrRoute.Id.fromString("Green-B"), 1, false),
                TripDetailsFilter("12345", null, null),
            ),
            deepLink?.sheetRoute,
        )
    }

    @Test
    fun `matches stop with expanded url`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Stop("place-chill", "Green-B", 1, "12345"),
            DeepLinkState.from(
                "${variant.backendRoot}/stop/place-chill/route/Green-B/direction/1/trip/12345"
            ),
        )
    }

    @Test
    fun `matches alert with route`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Alert("12345", "Red", null),
            DeepLinkState.from("${variant.backendRoot}/a/12345/r/Red"),
        )
    }

    @Test
    fun `matches alert with stop`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Alert("12345", null, "place-pktrm"),
            DeepLinkState.from("${variant.backendRoot}/a/12345/s/place-pktrm"),
        )
    }

    @Test
    fun `matches alert with expanded url`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertEquals(
            DeepLinkState.Alert("12345", null, "place-pktrm"),
            DeepLinkState.from("${variant.backendRoot}/alert/12345/stop/place-pktrm"),
        )
    }

    fun `returns null on unknown path`() = parametricTest {
        val variant: AppVariant = anyEnumValue()
        assertNull(DeepLinkState.from("${variant.backendRoot}/some-unknown-path"))
    }
}
