package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.overRouteColor
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import java.util.Locale

@Composable
fun ExplainerPage(type: ExplainerType, routeAccents: TripRouteAccents, goBack: () -> Unit) {
    Column(Modifier.background(colorResource(R.color.fill2))) {
        Row(
            Modifier.background(routeAccents.color).padding(16.dp).safeDrawingPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (icon, description) = routeIcon(routeAccents.type)
            Image(
                icon,
                description,
                Modifier.clearAndSetSemantics {}.size(24.dp),
                colorFilter = ColorFilter.tint(routeAccents.textColor),
            )
            Text(
                stringResource(R.string.details),
                color = routeAccents.textColor,
                style = Typography.headline,
                modifier = Modifier.weight(1f),
            )
            ActionButton(ActionButtonKind.Close, colors = ButtonDefaults.overRouteColor()) {
                goBack()
            }
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ExplanationHeadline(type, routeAccents.type)
            ExplanationImage(type, routeAccents)
            ExplanationText(type, routeAccents.type)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun ExplanationHeadline(type: ExplainerType, routeType: RouteType, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        when (type) {
            ExplainerType.FinishingAnotherTrip ->
                stringResource(R.string.explainer_headline_finishing_another)
            ExplainerType.NoPrediction -> stringResource(R.string.explainer_headline_no_prediction)
            ExplainerType.NoVehicle ->
                stringResource(
                        R.string.explainer_headline_no_vehicle,
                        routeType.typeText(context, true),
                    )
                    .replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
        },
        modifier,
        style = Typography.title2Bold,
    )
}

@Composable
fun ExplanationImage(
    type: ExplainerType,
    routeAccents: TripRouteAccents,
    modifier: Modifier = Modifier,
) {
    when (type) {
        ExplainerType.FinishingAnotherTrip ->
            Box(modifier.clearAndSetSemantics {}) {
                Image(
                    routeTurnaroundResource(routeAccents.type),
                    null,
                    Modifier.zIndex(1f).fillMaxWidth(),
                    colorFilter = ColorFilter.tint(routeAccents.textColor),
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painterResource(R.drawable.turnaround_shape),
                    null,
                    Modifier.fillMaxWidth(),
                    colorFilter = ColorFilter.tint(routeAccents.color),
                    contentScale = ContentScale.Fit,
                )
            }
        else -> {}
    }
}

@Composable
fun routeTurnaroundResource(routeType: RouteType) =
    when (routeType) {
        RouteType.BUS -> painterResource(R.drawable.turnaround_icon_bus)
        RouteType.COMMUTER_RAIL -> painterResource(R.drawable.turnaround_icon_cr)
        RouteType.FERRY -> painterResource(R.drawable.turnaround_icon_ferry)
        else -> painterResource(R.drawable.turnaround_icon_subway)
    }

@Composable
fun ExplanationText(type: ExplainerType, routeType: RouteType, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        when (type) {
            ExplainerType.FinishingAnotherTrip ->
                stringResource(
                        R.string.explainer_text_finishing_another,
                        routeType.typeText(context, true),
                    )
                    .replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
            ExplainerType.NoPrediction -> stringResource(R.string.explainer_text_no_prediction)
            ExplainerType.NoVehicle ->
                stringResource(
                        R.string.explainer_text_no_vehicle,
                        routeType.typeText(context, true),
                    )
                    .replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
        },
        modifier,
        style = Typography.body,
    )
}
