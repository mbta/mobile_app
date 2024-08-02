package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun RouteCardHeader(route: Route, body: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier.heightIn(min = 44.dp)
                .background(color = Color.fromHex(route.color))
                .fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = route.textColor
        val routeName =
            when (route.type) {
                RouteType.BUS -> route.shortName
                else -> route.longName
            }

        val routeIconResource =
            when (route.type) {
                RouteType.BUS -> painterResource(R.drawable.bus)
                RouteType.FERRY -> painterResource(R.drawable.ferry)
                RouteType.COMMUTER_RAIL -> painterResource(R.drawable.commuter_rail)
                else -> painterResource(R.drawable.subway)
            }

        val routeIconDescription =
            when (route.type) {
                RouteType.BUS -> "Bus"
                RouteType.FERRY -> "Ferry"
                RouteType.COMMUTER_RAIL -> "Commuter Rail"
                else -> "Subway"
            }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                routeIconResource,
                contentDescription = routeIconDescription,
                tint = Color.fromHex(textColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = routeName,
                maxLines = 1,
                style =
                    LocalTextStyle.current.copy(
                        color = Color.fromHex(textColor),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
            )
        }
    }
    body()
}
