package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.TestData

@Composable
fun FavoriteStopCard(
    stop: Stop,
    route: LineOrRoute,
    direction: Direction,
    toggleDirection: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onlyServingOppositeDirection: Boolean = false,
) {
    Column(modifier.haloContainer(2.dp)) {
        StopHeader(stop)
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoutePill(
                (route as? LineOrRoute.Route)?.route,
                (route as? LineOrRoute.Line)?.line,
                type = RoutePillType.Flex,
            )
            DirectionLabel(
                direction,
                Modifier.weight(1f),
                onlyServingOppositeDirection = onlyServingOppositeDirection,
            )
            if (toggleDirection != null) {
                ActionButton(
                    ActionButtonKind.Exchange,
                    size = 44.dp,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.halo),
                            contentColor = colorResource(R.color.text),
                        ),
                    action = toggleDirection,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FavoriteStopCardPreview() {
    val objects = TestData.clone()
    val wellington =
        objects.stop {
            id = "place-welln"
            locationType = LocationType.STATION
            name = "Wellington"
        }
    val wonderland =
        objects.stop {
            id = "place-wondl"
            locationType = LocationType.STATION
            name = "Wonderland"
        }
    val harvardStadiumGate2Inbound =
        objects.stop {
            id = "2551"
            locationType = LocationType.STOP
            name = "N Harvard St @ Gate 2 Harvard Stadium"
        }
    val forestHills =
        objects.stop {
            id = "place-forhl"
            locationType = LocationType.STATION
            name = "Forest Hills"
        }
    val boylston = objects.getStop("place-boyls")
    val ol = LineOrRoute.Route(objects.getRoute("Orange"))
    val bl =
        LineOrRoute.Route(
            objects.route {
                id = "Blue"
                color = "003DA5"
                directionDestinations = listOf("Bowdoin", "Wonderland")
                directionNames = listOf("West", "East")
                longName = "Blue Line"
                textColor = "FFFFFF"
                type = RouteType.HEAVY_RAIL
            }
        )
    val bus66 =
        LineOrRoute.Route(
            objects.route {
                id = "66"
                color = "FFC72C"
                directionDestinations = listOf("Harvard Square", "Nubian Station")
                directionNames = listOf("Outbound", "Inbound")
                shortName = "66"
                textColor = "000000"
                type = RouteType.BUS
            }
        )
    val gl =
        LineOrRoute.Line(
            objects.getLine("line-Green"),
            setOf(
                objects.getRoute("Green-B"),
                objects.getRoute("Green-C"),
                objects.getRoute("Green-D"),
                objects.getRoute("Green-E"),
            ),
        )
    val olSouthbound = Direction(0, ol.route, wellington)
    val blWestbound = Direction(0, bl.route, wonderland)
    val bus66Inbound = Direction(1, bus66.route, harvardStadiumGate2Inbound)
    val olNorthbound = Direction(1, ol.route, forestHills)
    val glWestbound = Direction("West", "Copley & West", 0)
    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill1)).padding(16.dp).width(390.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FavoriteStopCard(wellington, ol, olSouthbound, toggleDirection = {})
            FavoriteStopCard(wonderland, bl, blWestbound, toggleDirection = null)
            FavoriteStopCard(
                harvardStadiumGate2Inbound,
                bus66,
                bus66Inbound,
                toggleDirection = null,
            )
            FavoriteStopCard(
                forestHills,
                ol,
                olNorthbound,
                toggleDirection = null,
                onlyServingOppositeDirection = true,
            )
            FavoriteStopCard(boylston, gl, glWestbound, toggleDirection = {})
        }
    }
}
