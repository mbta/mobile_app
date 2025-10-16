package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.routeModeLabel
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun TransitHeader(
    transit: LineOrRoute,
    rightContent: (@Composable (textColor: Color) -> Unit)? = null,
) {
    val (modeIcon, modeDescription) = routeIcon(transit.sortRoute)
    TransitHeader(
        transit.name,
        routeType = transit.type,
        backgroundColor = Color.fromHex(transit.backgroundColor),
        textColor = Color.fromHex(transit.textColor),
        modeIcon = modeIcon,
        modeDescription = modeDescription,
        rightContent,
    )
}

@Composable
fun TransitHeader(
    name: String,
    routeType: RouteType,
    backgroundColor: Color,
    textColor: Color,
    modeIcon: Painter,
    modeDescription: String?,
    rightContent: (@Composable (textColor: Color) -> Unit)? = null,
) {
    val routeContentDescription = routeModeLabel(LocalContext.current, name, routeType)

    Row(
        modifier = Modifier.background(backgroundColor).fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modeIcon,
            contentDescription = modeDescription,
            tint = textColor,
            modifier = Modifier.placeholderIfLoading(),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = textColor,
            maxLines = 1,
            modifier =
                Modifier.semantics {
                        heading()
                        contentDescription = routeContentDescription
                    }
                    .weight(1.0f)
                    .placeholderIfLoading(),
            style = Typography.bodySemibold,
        )
        if (rightContent != null) {
            rightContent(textColor)
        }
    }
}
