package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutePillSpecTest {
    @Test
    fun `test bus`() = parametricTest {
        val busRoute = route {
            type = RouteType.BUS
            color = "FFC72C"
            shortName = "62/76"
            textColor = "000000"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val pill = RoutePillSpec(busRoute, null, type, height)

        val expectedWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "000000",
                "FFC72C",
                RoutePillSpec.Content.Text("62/76"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Rectangle,
            ),
            pill,
        )

        val searchStationPill =
            RoutePillSpec(busRoute, null, type, height, RoutePillSpec.Context.SearchStation)
        assertEquals(
            RoutePillSpec(
                "000000",
                "FFC72C",
                RoutePillSpec.Content.ModeImage(RouteType.BUS),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Rectangle,
            ),
            searchStationPill,
        )
    }

    @Test
    fun `test heavy rail`() = parametricTest {
        val redLine = route {
            type = RouteType.HEAVY_RAIL
            color = "DA291C"
            longName = "Red Line"
            textColor = "FFFFFF"
        }
        val blueLine = route {
            type = RouteType.HEAVY_RAIL
            color = "003DA5"
            longName = "Blue Line"
            textColor = "FFFFFF"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val redLinePill = RoutePillSpec(redLine, null, type, height)
        val blueLinePill = RoutePillSpec(blueLine, null, type, height)

        val expectedWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("RL"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            redLinePill,
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "003DA5",
                RoutePillSpec.Content.Text("BL"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            blueLinePill,
        )
    }

    @Test
    fun `test light rail`() = parametricTest {
        val greenLineC = route {
            id = "Green-C"
            type = RouteType.LIGHT_RAIL
            color = "00843D"
            longName = "Green Line C"
            shortName = "C"
            textColor = "FFFFFF"
        }
        val mattapan = route {
            id = "Mattapan"
            type = RouteType.LIGHT_RAIL
            color = "DA291C"
            longName = "Mattapan Line"
            textColor = "FFFFFF"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val greenLineCPill = RoutePillSpec(greenLineC, null, type, height)
        val mattapanPill = RoutePillSpec(mattapan, null, type, height)

        val expectedContent =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Content.Text("GL C")
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Content.Text("C")
            }
        val expectedGLWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Circle
            }
        val expectedMattapanWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                expectedContent,
                height,
                expectedGLWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            greenLineCPill,
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("M"),
                height,
                expectedMattapanWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            mattapanPill,
        )
    }

    @Test
    fun `test commuter rail`() = parametricTest {
        val middleborough = route {
            type = RouteType.COMMUTER_RAIL
            color = "80276C"
            longName = "Middleborough/Lakeville Line"
            textColor = "FFFFFF"
        }
        val providence = route {
            type = RouteType.COMMUTER_RAIL
            color = "80276C"
            longName = "Providence/Stoughton Line"
            textColor = "FFFFFF"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val middleboroughPill = RoutePillSpec(middleborough, null, type, height)
        val providencePill = RoutePillSpec(providence, null, type, height)

        val modePillContent =
            when (type) {
                RoutePillSpec.Type.Fixed,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Content.Text("CR")
                RoutePillSpec.Type.Flex -> null
            }
        val expectedWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                modePillContent ?: RoutePillSpec.Content.Text("Middleborough/Lakeville"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            middleboroughPill,
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                modePillContent ?: RoutePillSpec.Content.Text("Providence/Stoughton"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            providencePill,
        )
    }

    @Test
    fun `test ferry`() = parametricTest {
        val ferry = route {
            type = RouteType.FERRY
            color = "008EAA"
            longName = "Hingham/Hull Ferry"
            textColor = "FFFFFF"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val ferryPill = RoutePillSpec(ferry, null, type, height)

        val modePillContent =
            when (type) {
                RoutePillSpec.Type.Fixed,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Content.ModeImage(RouteType.FERRY)
                RoutePillSpec.Type.Flex -> null
            }
        val expectedWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "008EAA",
                modePillContent ?: RoutePillSpec.Content.Text("Hingham/Hull Ferry"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            ferryPill,
        )
    }

    @Test
    fun `test lines`() = parametricTest {
        val redLine = line {
            id = "line-Red"
            color = "DA291C"
            longName = "Red Line"
            textColor = "FFFFFF"
        }
        val greenLine = line {
            id = "line-Green"
            color = "00843D"
            longName = "Green Line"
            textColor = "FFFFFF"
        }

        val type: RoutePillSpec.Type = anyEnumValue()
        val height: RoutePillSpec.Height = anyEnumValue()
        val rlPill = RoutePillSpec(null, redLine, type, height)
        val glPill = RoutePillSpec(null, greenLine, type, height)

        val expectedWidth =
            when (type) {
                RoutePillSpec.Type.Fixed -> RoutePillSpec.Width.Fixed
                RoutePillSpec.Type.Flex,
                RoutePillSpec.Type.FlexCompact -> RoutePillSpec.Width.Flex
            }
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("Red Line"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            rlPill,
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("GL"),
                height,
                expectedWidth,
                RoutePillSpec.Shape.Capsule,
            ),
            glPill,
        )
    }
}
