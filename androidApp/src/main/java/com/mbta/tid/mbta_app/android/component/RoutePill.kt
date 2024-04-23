package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun RoutePill(route: Route) {
    val textColor = route.textColor
    val routeColor = route.color
    val pillText =
        when (route.type) {
            RouteType.BUS -> route.shortName
            else -> route.longName
        }

    Text(
        text = pillText.uppercase(),
        modifier =
            Modifier.sizeIn(minWidth = 25.dp)
                .background(Color.fromHex(routeColor), RoundedCornerShape(100))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        maxLines = 1,
        style =
            LocalTextStyle.current.copy(
                color = Color.fromHex(textColor),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun RoutePillPreview() {
    val routes =
        listOf(
            route {
                type = RouteType.BUS
                color = "FFC72C"
                longName = "Houghs Neck - Quincy Center Station via Germantown"
                shortName = "216"
                textColor = "000000"
            },
            route {
                type = RouteType.BUS
                color = "FFC72C"
                longName = "Harvard Square - Nubian Station"
                shortName = "1"
                textColor = "000000"
            },
            route {
                type = RouteType.BUS
                color = "FFC72C"
                longName = "Bedford VA Hospital - Alewife Station via Hanscom Airport"
                shortName = "62/76"
                textColor = "000000"
            },
            route {
                type = RouteType.HEAVY_RAIL
                color = "DA291C"
                longName = "Red Line"
                shortName = ""
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.HEAVY_RAIL
                color = "003DA5"
                longName = "Blue Line"
                shortName = ""
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.LIGHT_RAIL
                color = "00843D"
                longName = "Green Line C"
                shortName = "C"
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.COMMUTER_RAIL
                color = "80276C"
                longName = "Middleborough/Lakeville Line"
                shortName = ""
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.COMMUTER_RAIL
                color = "80276C"
                longName = "Providence/Stoughton Line"
                shortName = ""
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.FERRY
                color = "008EAA"
                longName = "Hingham/Hull Ferry"
                shortName = ""
                textColor = "FFFFFF"
            },
            route {
                type = RouteType.BUS
                color = "FFC72C"
                longName = "Kendall/MIT - Broadway via Downtown Crossing"
                shortName = "Red Line Shuttle"
                textColor = "000000"
            },
            route {
                type = RouteType.BUS
                color = "FFC72C"
                longName = "Brookline Hills - Kenmore"
                shortName = "Green Line D Shuttle"
                textColor = "000000"
            }
        )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (route in routes) {
            RoutePill(route)
        }
    }
}
