package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.StarButton
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop

@Composable
fun StopDetailsFilteredHeader(
    route: Route?,
    line: Line?,
    stop: Stop?,
    pinned: Boolean? = false,
    onPin: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
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
            if (line != null) {
                RoutePill(
                    route = null,
                    line = line,
                    type = RoutePillType.Fixed,
                    modifier = Modifier.placeholderIfLoading(),
                )
            } else if (route != null) {
                RoutePill(
                    route = route,
                    type = RoutePillType.Fixed,
                    modifier = Modifier.placeholderIfLoading(),
                )
            }
            if (stop != null) {
                Text(
                    AnnotatedString.fromHtml(stringResource(R.string.header_at_stop, stop.name)),
                    modifier = Modifier.semantics { heading() }.weight(1f).placeholderIfLoading(),
                    style = Typography.headline,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onPin != null) {
                StarButton(pinned, colorResource(R.color.text), onPin)
            }
            if (onClose != null) {
                ActionButton(ActionButtonKind.Close) { onClose() }
            }
        }
    }
}

@Preview
@Composable
private fun StopDetailsFilteredHeaderPreview() {
    val objects = ObjectCollectionBuilder()
    val route =
        objects.route {
            color = "ED8B00"
            type = RouteType.HEAVY_RAIL
            longName = "Orange Line"
        }
    val stop = objects.stop { name = "Back Bay" }

    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill2))) {
            StopDetailsFilteredHeader(
                route = route,
                line = null,
                stop = stop,
                pinned = true,
                onPin = {},
                onClose = {},
            )
            HorizontalDivider()
            StopDetailsFilteredHeader(
                route = route,
                line = null,
                stop = stop,
                pinned = true,
                onPin = {},
            )
        }
    }
}
