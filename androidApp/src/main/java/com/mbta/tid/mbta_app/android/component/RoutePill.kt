package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType

typealias RoutePillType = RoutePillSpec.Type

@Composable
fun RoutePill(
    route: Route?,
    line: Line? = null,
    type: RoutePillType,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spec = RoutePillSpec(route, line, type)
    val textColor = Color.fromHex(spec.textColor)
    val routeColor = Color.fromHex(spec.routeColor)

    val pillContent = spec.content

    val shape =
        when (spec.shape) {
            RoutePillSpec.Shape.Rectangle -> RoundedCornerShape(4.dp)
            RoutePillSpec.Shape.Capsule -> RoundedCornerShape(percent = 100)
        }

    val fontSize =
        when (spec.size) {
            RoutePillSpec.Size.CircleSmall,
            RoutePillSpec.Size.FlexPillSmall -> 12.sp
            else -> 16.sp
        }

    val iconSize =
        when (spec.size) {
            RoutePillSpec.Size.CircleSmall,
            RoutePillSpec.Size.FlexPillSmall -> 16.dp
            else -> 24.dp
        }

    fun Modifier.withSizePadding() =
        when (spec.size) {
            RoutePillSpec.Size.FixedPill -> size(width = 50.dp, height = 24.dp)
            RoutePillSpec.Size.Circle -> size(24.dp)
            RoutePillSpec.Size.CircleSmall -> size(16.dp)
            RoutePillSpec.Size.FlexPill ->
                height(24.dp).padding(horizontal = 12.dp).widthIn(min = (48 - 12 * 2).dp)
            RoutePillSpec.Size.FlexPillSmall ->
                height(16.dp).padding(horizontal = 8.dp).widthIn(min = (36 - 8 * 2).dp)
        }

    fun Modifier.withColor() =
        if (isActive) {
            background(routeColor, shape)
        } else {
            border(1.dp, routeColor, shape).padding(1.dp)
        }

    val typeText = route?.type?.typeText(LocalContext.current, true)

    val finalModifier =
        modifier
            .placeholderIfLoading()
            .withColor()
            .withSizePadding()
            .wrapContentHeight(Alignment.CenterVertically)
            .semantics {
                contentDescription = "${route?.label ?: line?.longName ?: ""} ${typeText ?: ""}"
            }

    when (pillContent) {
        RoutePillSpec.Content.Empty -> {}
        is RoutePillSpec.Content.Text ->
            Text(
                if (route?.type == RouteType.COMMUTER_RAIL) pillContent.text
                else pillContent.text.uppercase(),
                modifier = finalModifier,
                color = if (isActive) textColor else Color.Unspecified,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        is RoutePillSpec.Content.ModeImage -> {
            val (painter, contentDescription) = routeIcon(routeType = pillContent.mode)
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = finalModifier.size(iconSize),
                tint = if (isActive) textColor else LocalContentColor.current
            )
        }
    }
}

@Preview
@Composable
private fun RoutePillPreviews() {
    @Composable
    fun RoutePillPreview(route: Route, line: Line? = null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutePill(route = route, line = line, type = RoutePillType.Fixed, isActive = false)
            RoutePill(route = route, line = line, type = RoutePillType.Fixed)
            RoutePill(route = route, line = line, type = RoutePillType.Flex)
            RoutePill(route = route, line = line, type = RoutePillType.FlexCompact)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoutePillPreview(
            route =
                Route(
                    id = "Red",
                    type = RouteType.HEAVY_RAIL,
                    color = "DA291C",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
                    longName = "Red Line",
                    shortName = "",
                    sortOrder = 10010,
                    textColor = "FFFFFF",
                    lineId = "line-Red",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Orange",
                    type = RouteType.HEAVY_RAIL,
                    color = "ED8B00",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Forest Hills", "Oak Grove"),
                    longName = "Orange Line",
                    shortName = "",
                    sortOrder = 10020,
                    textColor = "FFFFFF",
                    lineId = "line-Orange",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Blue",
                    type = RouteType.HEAVY_RAIL,
                    color = "003DA5",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Bowdoin", "Wonderland"),
                    longName = "Blue Line",
                    shortName = "",
                    sortOrder = 10040,
                    textColor = "FFFFFF",
                    lineId = "line-Blue",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Mattapan",
                    type = RouteType.LIGHT_RAIL,
                    color = "DA291C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Mattapan", "Ashmont"),
                    longName = "Mattapan Trolley",
                    shortName = "",
                    sortOrder = 10011,
                    textColor = "FFFFFF",
                    lineId = "line-Mattapan",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Green-B",
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Boston College", "Government Center"),
                    longName = "Green Line B",
                    shortName = "B",
                    sortOrder = 10032,
                    textColor = "FFFFFF",
                    lineId = "line-Green",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Green-C",
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Cleveland Circle", "Government Center"),
                    longName = "Green Line C",
                    shortName = "C",
                    sortOrder = 10033,
                    textColor = "FFFFFF",
                    lineId = "line-Green",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Green-D",
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Riverside", "Union Square"),
                    longName = "Green Line D",
                    shortName = "D",
                    sortOrder = 10034,
                    textColor = "FFFFFF",
                    lineId = "line-Green",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Green-E",
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Heath Street", "Medford/Tufts"),
                    longName = "Green Line E",
                    shortName = "E",
                    sortOrder = 10035,
                    textColor = "FFFFFF",
                    lineId = "line-Green",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "CR-Fitchburg",
                    type = RouteType.COMMUTER_RAIL,
                    color = "80276C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Wachusett", "North Station"),
                    longName = "Fitchburg Line",
                    shortName = "",
                    sortOrder = 20012,
                    textColor = "FFFFFF",
                    lineId = "line-Fitchburg",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "216",
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Houghs Neck", "Quincy Center Station"),
                    longName = "Houghs Neck - Quincy Center Station via Germantown",
                    shortName = "216",
                    sortOrder = 52160,
                    textColor = "000000",
                    lineId = "line-214216",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "627",
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Bedford VA Hospital", "Alewife Station"),
                    longName = "Bedford VA Hospital - Alewife Station via Hanscom Airport",
                    shortName = "62/76",
                    sortOrder = 50621,
                    textColor = "000000",
                    lineId = "line-6276",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "741",
                    type = RouteType.BUS,
                    color = "7C878E",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Logan Airport Terminals", "South Station"),
                    longName = "Logan Airport Terminals - South Station",
                    shortName = "SL1",
                    sortOrder = 10051,
                    textColor = "FFFFFF",
                    lineId = "line-SLWaterfront",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Boat-F1",
                    type = RouteType.FERRY,
                    color = "008EAA",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Hingham or Hull", "Long Wharf or Rowes Wharf"),
                    longName = "Hingham/Hull Ferry",
                    shortName = "",
                    sortOrder = 30002,
                    textColor = "FFFFFF",
                    lineId = "line-Boat-F1",
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = "Shuttle-BroadwayKendall",
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
                    longName = "Kendall/MIT - Broadway via Downtown Crossing",
                    shortName = "Red Line Shuttle",
                    sortOrder = 61050,
                    textColor = "000000",
                    lineId = "line-Red",
                ),
            line =
                Line(
                    id = "line-Red",
                    color = "DA291C",
                    longName = "Red Line",
                    shortName = "",
                    sortOrder = 10010,
                    textColor = "FFFFFF"
                )
        )
    }
}
