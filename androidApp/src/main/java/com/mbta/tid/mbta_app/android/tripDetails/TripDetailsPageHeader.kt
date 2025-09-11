package com.mbta.tid.mbta_app.android.tripDetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillHeight
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.routeModeLabel
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.TestData

@Composable
fun TripDetailsPageHeader(route: Route?, direction: Direction?, onClose: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).padding(top = 4.dp).semantics(mergeDescendants = true) {
                heading()
            },
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically,
        ) {
            val pillDescription = routeModeLabel(LocalContext.current, line = null, route)
            if (route != null) {
                RoutePill(
                    route = route,
                    type = RoutePillType.FlexCompact,
                    height = RoutePillHeight.Large,
                    modifier =
                        Modifier.semantics { contentDescription = pillDescription }
                            .placeholderIfLoading(),
                    border =
                        BorderStroke(
                            2.dp,
                            colorResource(
                                    if (route.id == "Blue" || route.type == RouteType.COMMUTER_RAIL)
                                        R.color.halo_dark
                                    else R.color.halo_light
                                )
                                .copy(alpha = 0.6f),
                        ),
                )
            }
            if (direction != null) {
                DirectionLabel(
                    direction,
                    modifier = Modifier.semantics { heading() }.weight(1f).placeholderIfLoading(),
                    textColor = route?.textColor?.let { Color.fromHex(it) } ?: Color.Unspecified,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        ActionButton(ActionButtonKind.Close) { onClose() }
    }
}

@Preview
@Composable
private fun TripDetailsPageHeaderPreview() {
    val objects = TestData.clone()

    @Composable
    fun HeaderPreview(route: Route, directionId: Int) {
        val direction =
            Direction(
                route.directionNames[directionId],
                route.directionDestinations[directionId],
                directionId,
            )
        Box(Modifier.background(Color.fromHex(route.color))) {
            TripDetailsPageHeader(route, direction, onClose = {})
        }
    }

    @Composable
    fun HeaderPreview(routeId: String, directionId: Int) {
        val route = objects.getRoute(routeId)
        HeaderPreview(route, directionId)
    }

    MyApplicationTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderPreview("Green-C", 0)
            HeaderPreview("Red", 0)
            HeaderPreview("Orange", 1)
            HeaderPreview(
                Single.route {
                    id = "Blue"
                    color = "003DA5"
                    directionDestinations = listOf("Bowdoin", "Wonderland")
                    directionNames = listOf("West", "East")
                    longName = "Blue Line"
                    textColor = "FFFFFF"
                    type = RouteType.HEAVY_RAIL
                },
                1,
            )
            HeaderPreview("743", 1)
            HeaderPreview("15", 0)
            HeaderPreview("CR-Newburyport", 0)
            HeaderPreview(
                Single.route {
                    color = "008EAA"
                    directionDestinations = listOf("Hingham or Hull", "Long Wharf or Rowes Wharf")
                    directionNames = listOf("Outbound", "Inbound")
                    longName = "Hingham/Hull Ferry"
                    textColor = "FFFFFF"
                    type = RouteType.FERRY
                },
                1,
            )
        }
    }
}
