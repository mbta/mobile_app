package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.routeModeLabel
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType

typealias RoutePillType = RoutePillSpec.Type

typealias RoutePillHeight = RoutePillSpec.Height

typealias RoutePillWidth = RoutePillSpec.Width

@Composable
fun RoutePill(
    route: Route?,
    line: Line? = null,
    type: RoutePillType,
    height: RoutePillHeight = RoutePillHeight.Medium,
    isActive: Boolean = true,
    contentDescription: RoutePillSpec.ContentDescription? = null,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    RoutePill(
        route,
        line,
        isActive,
        RoutePillSpec(route, line, type, height, contentDescription = contentDescription),
        modifier,
        border,
    )
}

@Composable
fun RoutePill(
    route: Route?,
    line: Line? = null,
    isActive: Boolean = true,
    spec: RoutePillSpec,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    val textColor = Color.fromHex(spec.textColor)
    val routeColor = Color.fromHex(spec.routeColor)

    val pillContent = spec.content

    val shape =
        when (spec.shape) {
            RoutePillSpec.Shape.Rectangle -> RoundedCornerShape(4.dp)
            RoutePillSpec.Shape.Capsule -> RoundedCornerShape(percent = 100)
        }

    val borderShape =
        when (spec.shape) {
            RoutePillSpec.Shape.Rectangle ->
                RoundedCornerShape(border?.width?.let { 4.dp + it } ?: 4.dp)
            RoutePillSpec.Shape.Capsule -> RoundedCornerShape(percent = 100)
        }

    val fontSize =
        when (spec.height) {
            RoutePillHeight.Small -> 12.sp
            RoutePillHeight.Medium -> 16.sp
            RoutePillHeight.Large -> if (spec.width == RoutePillWidth.Circle) 20.sp else 16.sp
        }

    val pillHeight =
        when (spec.height) {
            RoutePillHeight.Small -> 16.dp
            RoutePillHeight.Medium -> 24.dp
            RoutePillHeight.Large -> if (spec.width == RoutePillWidth.Circle) 32.dp else 24.dp
        }

    fun Modifier.withSizePadding() =
        when (spec.width) {
            RoutePillWidth.Fixed -> size(width = (pillHeight + 1.dp) * 2, height = pillHeight)
            RoutePillWidth.Circle -> size(pillHeight)
            RoutePillWidth.Flex ->
                height(pillHeight)
                    .padding(horizontal = pillHeight / 2)
                    .widthIn(min = pillHeight / 2 + 12.dp)
        }

    fun Modifier.withColor() =
        if (isActive) {
            (if (border != null) this.border(border, borderShape).padding(border.width) else this)
                .background(routeColor, shape)
                .clip(shape)
        } else {
            border(1.dp, routeColor, shape).padding(1.dp)
        }

    val contentDescription =
        spec.contentDescription?.text ?: routeModeLabel(LocalContext.current, line, route)

    val finalModifier =
        modifier
            .placeholderIfLoading()
            .withColor()
            .withSizePadding()
            .wrapContentHeight(Alignment.CenterVertically)
            .semantics { this.contentDescription = contentDescription }

    when (pillContent) {
        RoutePillSpec.Content.Empty -> {}
        is RoutePillSpec.Content.Text ->
            BasicText(
                if (route?.type == RouteType.COMMUTER_RAIL) pillContent.text
                else pillContent.text.uppercase(),
                modifier = finalModifier,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(8.sp, fontSize, 0.25.sp),
                style =
                    TextStyle(
                        color = if (isActive) textColor else Color.Unspecified,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                    ),
            )

        is RoutePillSpec.Content.ModeImage -> {
            val painter = routeIcon(routeType = pillContent.mode)
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = finalModifier.size(pillHeight),
                tint = if (isActive) textColor else LocalContentColor.current,
            )
        }
    }
}

val RoutePillSpec.ContentDescription.text: String
    @Composable
    get() =
        when (this) {
            is RoutePillSpec.ContentDescription.StopSearchResultRoute ->
                routeModeLabel(LocalContext.current, routeName, routeType, isOnly)
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
            RoutePill(
                route = route,
                line = line,
                type = RoutePillType.FlexCompact,
                height = RoutePillHeight.Small,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Red"),
                    type = RouteType.HEAVY_RAIL,
                    color = "DA291C",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
                    isListedRoute = true,
                    longName = "Red Line",
                    shortName = "",
                    sortOrder = 10010,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Red"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Orange"),
                    type = RouteType.HEAVY_RAIL,
                    color = "ED8B00",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Forest Hills", "Oak Grove"),
                    isListedRoute = true,
                    longName = "Orange Line",
                    shortName = "",
                    sortOrder = 10020,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Orange"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Blue"),
                    type = RouteType.HEAVY_RAIL,
                    color = "003DA5",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Bowdoin", "Wonderland"),
                    isListedRoute = true,
                    longName = "Blue Line",
                    shortName = "",
                    sortOrder = 10040,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Blue"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Mattapan"),
                    type = RouteType.LIGHT_RAIL,
                    color = "DA291C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Mattapan", "Ashmont"),
                    isListedRoute = true,
                    longName = "Mattapan Trolley",
                    shortName = "",
                    sortOrder = 10011,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Mattapan"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Green-B"),
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Boston College", "Government Center"),
                    isListedRoute = true,
                    longName = "Green Line B",
                    shortName = "B",
                    sortOrder = 10032,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Green"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Green-C"),
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Cleveland Circle", "Government Center"),
                    isListedRoute = true,
                    longName = "Green Line C",
                    shortName = "C",
                    sortOrder = 10033,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Green"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Green-D"),
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Riverside", "Union Square"),
                    isListedRoute = true,
                    longName = "Green Line D",
                    shortName = "D",
                    sortOrder = 10034,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Green"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Green-E"),
                    type = RouteType.LIGHT_RAIL,
                    color = "00843D",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Heath Street", "Medford/Tufts"),
                    isListedRoute = true,
                    longName = "Green Line E",
                    shortName = "E",
                    sortOrder = 10035,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Green"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("CR-Fitchburg"),
                    type = RouteType.COMMUTER_RAIL,
                    color = "80276C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Wachusett", "North Station"),
                    isListedRoute = true,
                    longName = "Fitchburg Line",
                    shortName = "",
                    sortOrder = 20012,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Fitchburg"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("216"),
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Houghs Neck", "Quincy Center Station"),
                    isListedRoute = true,
                    longName = "Houghs Neck - Quincy Center Station via Germantown",
                    shortName = "216",
                    sortOrder = 52160,
                    textColor = "000000",
                    lineId = Line.Id("line-214216"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("627"),
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Bedford VA Hospital", "Alewife Station"),
                    isListedRoute = true,
                    longName = "Bedford VA Hospital - Alewife Station via Hanscom Airport",
                    shortName = "62/76",
                    sortOrder = 50621,
                    textColor = "000000",
                    lineId = Line.Id("line-6276"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("741"),
                    type = RouteType.BUS,
                    color = "7C878E",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Logan Airport Terminals", "South Station"),
                    isListedRoute = true,
                    longName = "Logan Airport Terminals - South Station",
                    shortName = "SL1",
                    sortOrder = 10051,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-SLWaterfront"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Boat-F1"),
                    type = RouteType.FERRY,
                    color = "008EAA",
                    directionNames = listOf("Outbound", "Inbound"),
                    directionDestinations = listOf("Hingham or Hull", "Long Wharf or Rowes Wharf"),
                    isListedRoute = true,
                    longName = "Hingham/Hull Ferry",
                    shortName = "",
                    sortOrder = 30002,
                    textColor = "FFFFFF",
                    lineId = Line.Id("line-Boat-F1"),
                )
        )
        RoutePillPreview(
            route =
                Route(
                    id = Route.Id("Shuttle-BroadwayKendall"),
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("South", "North"),
                    directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
                    isListedRoute = true,
                    longName = "Kendall/MIT - Broadway via Downtown Crossing",
                    shortName = "Red Line Shuttle",
                    sortOrder = 61050,
                    textColor = "000000",
                    lineId = Line.Id("line-Red"),
                ),
            line =
                Line(
                    id = Line.Id("line-Red"),
                    color = "DA291C",
                    longName = "Red Line",
                    shortName = "",
                    sortOrder = 10010,
                    textColor = "FFFFFF",
                ),
        )
    }
}
