package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.StarButton
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.routeModeLabel
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.NavigationCallbacks

@Composable
fun StopDetailsFilteredHeader(
    route: Route?,
    line: Line?,
    stop: Stop?,
    isFavorite: Boolean? = false,
    onFavorite: (() -> Unit)? = null,
    navCallbacks: NavigationCallbacks,
) {
    SheetHeader(
        Modifier.padding(bottom = 12.dp),
        title = {
            Row(
                Modifier.weight(1f)
                    .semantics(mergeDescendants = true) { heading() }
                    .align(Alignment.CenterVertically),
                Arrangement.spacedBy(8.dp),
                Alignment.CenterVertically,
            ) {
                val pillDescription = routeModeLabel(LocalResources.current, line, route)
                if (line != null) {
                    RoutePill(
                        route = null,
                        line = line,
                        type = RoutePillType.Fixed,
                        modifier =
                            Modifier.semantics { contentDescription = pillDescription }
                                .placeholderIfLoading(),
                    )
                } else if (route != null) {
                    RoutePill(
                        route = route,
                        type = RoutePillType.Fixed,
                        modifier =
                            Modifier.semantics { contentDescription = pillDescription }
                                .placeholderIfLoading(),
                    )
                }
                if (stop != null) {
                    Text(
                        AnnotatedString.fromHtml(
                            stringResource(R.string.header_at_stop, stop.name)
                        ),
                        modifier =
                            Modifier.semantics { heading() }.weight(1f).placeholderIfLoading(),
                        style = Typography.headline,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        },
        navCallbacks = navCallbacks,
        rightActionContents = {
            if (onFavorite != null) {
                StarButton(isFavorite, colorResource(R.color.text), onFavorite)
            }
        },
        buttonColors = ButtonDefaults.contrastTranslucent(),
    )
}

@Preview
@Composable
private fun StopDetailsFilteredHeaderPreview() {
    val objects = ObjectCollectionBuilder("StopDetailsFilteredHeaderPreview")
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
                isFavorite = true,
                onFavorite = {},
                navCallbacks =
                    NavigationCallbacks(
                        onBack = {},
                        onClose = {},
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
            HorizontalDivider()
            StopDetailsFilteredHeader(
                route = route,
                line = null,
                stop = stop,
                isFavorite = true,
                onFavorite = {},
                navCallbacks =
                    NavigationCallbacks(
                        onBack = null,
                        onClose = null,
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
        }
    }
}
