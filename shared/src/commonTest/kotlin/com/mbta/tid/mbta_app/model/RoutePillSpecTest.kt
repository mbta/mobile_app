package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutePillSpecTest {
    @Test
    fun `test bus`() {
        val busRoute = route {
            type = RouteType.BUS
            color = "FFC72C"
            shortName = "62/76"
            textColor = "000000"
        }

        val fixedPill = RoutePillSpec(busRoute, null, RoutePillSpec.Type.Fixed)
        assertEquals(
            RoutePillSpec(
                "000000",
                "FFC72C",
                RoutePillSpec.Content.Text("62/76"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Rectangle
            ),
            fixedPill
        )
        val flexPill = RoutePillSpec(busRoute, null, RoutePillSpec.Type.Flex)
        assertEquals(
            RoutePillSpec(
                "000000",
                "FFC72C",
                RoutePillSpec.Content.Text("62/76"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Rectangle
            ),
            flexPill
        )

        val searchStationPill =
            RoutePillSpec(
                busRoute,
                null,
                RoutePillSpec.Type.FlexCompact,
                RoutePillSpec.Context.SearchStation
            )
        assertEquals(
            RoutePillSpec(
                "000000",
                "FFC72C",
                RoutePillSpec.Content.ModeImage(RouteType.BUS),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Rectangle
            ),
            searchStationPill
        )
    }

    @Test
    fun `test heavy rail`() {
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

        val redLineFixed = RoutePillSpec(redLine, null, RoutePillSpec.Type.Fixed)
        val redLineFlex = RoutePillSpec(redLine, null, RoutePillSpec.Type.Flex)
        val redLineFlexCompact = RoutePillSpec(redLine, null, RoutePillSpec.Type.FlexCompact)
        val blueLineFixed = RoutePillSpec(blueLine, null, RoutePillSpec.Type.Fixed)
        val blueLineFlex = RoutePillSpec(blueLine, null, RoutePillSpec.Type.Flex)
        val blueLineFlexCompact = RoutePillSpec(blueLine, null, RoutePillSpec.Type.FlexCompact)

        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("RL"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            redLineFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("RL"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            redLineFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("RL"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            redLineFlexCompact
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "003DA5",
                RoutePillSpec.Content.Text("BL"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            blueLineFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "003DA5",
                RoutePillSpec.Content.Text("BL"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            blueLineFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "003DA5",
                RoutePillSpec.Content.Text("BL"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            blueLineFlexCompact
        )
    }

    @Test
    fun `test light rail`() {
        val greenLineC = route {
            type = RouteType.LIGHT_RAIL
            color = "00843D"
            longName = "Green Line C"
            shortName = "C"
            textColor = "FFFFFF"
        }
        val mattapan = route {
            type = RouteType.LIGHT_RAIL
            color = "DA291C"
            longName = "Mattapan Trolley"
            textColor = "FFFFFF"
        }

        val greenLineCFixed = RoutePillSpec(greenLineC, null, RoutePillSpec.Type.Fixed)
        val greenLineCFlex = RoutePillSpec(greenLineC, null, RoutePillSpec.Type.Flex)
        val greenLineCFlexCompact = RoutePillSpec(greenLineC, null, RoutePillSpec.Type.FlexCompact)
        val mattapanFixed = RoutePillSpec(mattapan, null, RoutePillSpec.Type.Fixed)
        val mattapanFlex = RoutePillSpec(mattapan, null, RoutePillSpec.Type.Flex)
        val mattapanFlexCompact = RoutePillSpec(mattapan, null, RoutePillSpec.Type.FlexCompact)

        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("GL C"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            greenLineCFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("C"),
                RoutePillSpec.Size.Circle,
                RoutePillSpec.Shape.Capsule
            ),
            greenLineCFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("C"),
                RoutePillSpec.Size.CircleSmall,
                RoutePillSpec.Shape.Capsule
            ),
            greenLineCFlexCompact
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("M"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            mattapanFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("M"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            mattapanFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("M"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            mattapanFlexCompact
        )
    }

    @Test
    fun `test commuter rail`() {
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

        val middleboroughFixed = RoutePillSpec(middleborough, null, RoutePillSpec.Type.Fixed)
        val middleboroughFlex = RoutePillSpec(middleborough, null, RoutePillSpec.Type.Flex)
        val middleboroughFlexCompact =
            RoutePillSpec(middleborough, null, RoutePillSpec.Type.FlexCompact)
        val providenceFixed = RoutePillSpec(providence, null, RoutePillSpec.Type.Fixed)
        val providenceFlex = RoutePillSpec(providence, null, RoutePillSpec.Type.Flex)
        val providenceFlexCompact = RoutePillSpec(providence, null, RoutePillSpec.Type.FlexCompact)

        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("CR"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            middleboroughFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("Middleborough/Lakeville"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            middleboroughFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("CR"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            middleboroughFlexCompact
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("CR"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            providenceFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("Providence/Stoughton"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            providenceFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "80276C",
                RoutePillSpec.Content.Text("CR"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            providenceFlexCompact
        )
    }

    @Test
    fun `test ferry`() {
        val ferry = route {
            type = RouteType.FERRY
            color = "008EAA"
            longName = "Hingham/Hull Ferry"
            textColor = "FFFFFF"
        }

        val ferryFixed = RoutePillSpec(ferry, null, RoutePillSpec.Type.Fixed)
        val ferryFlex = RoutePillSpec(ferry, null, RoutePillSpec.Type.Flex)
        val ferryFlexCompact = RoutePillSpec(ferry, null, RoutePillSpec.Type.FlexCompact)

        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "008EAA",
                RoutePillSpec.Content.ModeImage(RouteType.FERRY),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            ferryFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "008EAA",
                RoutePillSpec.Content.Text("Hingham/Hull Ferry"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            ferryFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "008EAA",
                RoutePillSpec.Content.ModeImage(RouteType.FERRY),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            ferryFlexCompact
        )
    }

    @Test
    fun `test lines`() {
        val redLine = line {
            color = "DA291C"
            longName = "Red Line"
            textColor = "FFFFFF"
        }
        val greenLine = line {
            color = "00843D"
            longName = "Green Line"
            textColor = "FFFFFF"
        }

        val rlFixed = RoutePillSpec(null, redLine, RoutePillSpec.Type.Fixed)
        val rlFlex = RoutePillSpec(null, redLine, RoutePillSpec.Type.Flex)
        val rlFlexCompact = RoutePillSpec(null, redLine, RoutePillSpec.Type.FlexCompact)
        val glFixed = RoutePillSpec(null, greenLine, RoutePillSpec.Type.Fixed)
        val glFlex = RoutePillSpec(null, greenLine, RoutePillSpec.Type.Flex)
        val glFlexCompact = RoutePillSpec(null, greenLine, RoutePillSpec.Type.FlexCompact)

        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("Red Line"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            rlFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("Red Line"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            rlFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "DA291C",
                RoutePillSpec.Content.Text("Red Line"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            rlFlexCompact
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("GL"),
                RoutePillSpec.Size.FixedPill,
                RoutePillSpec.Shape.Capsule
            ),
            glFixed
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("GL"),
                RoutePillSpec.Size.FlexPill,
                RoutePillSpec.Shape.Capsule
            ),
            glFlex
        )
        assertEquals(
            RoutePillSpec(
                "FFFFFF",
                "00843D",
                RoutePillSpec.Content.Text("GL"),
                RoutePillSpec.Size.FlexPillSmall,
                RoutePillSpec.Shape.Capsule
            ),
            glFlexCompact
        )
    }
}
