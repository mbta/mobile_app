package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val pillText =
            when (route.type) {
                RouteType.BUS -> route.shortName
                else -> route.longName
            }

        Text(
            text = pillText.uppercase(),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            style =
                LocalTextStyle.current.copy(
                    color = Color.fromHex(textColor),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
        )
    }
    body()
}
