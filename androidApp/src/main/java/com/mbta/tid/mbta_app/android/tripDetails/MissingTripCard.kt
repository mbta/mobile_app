package com.mbta.tid.mbta_app.android.tripDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.utils.TestData

enum class MissingTripCardType {
    Complete,
    NotAvailable,
}

@Composable
fun MissingTripCard(
    type: MissingTripCardType,
    routeAccents: TripRouteAccents,
    modifier: Modifier = Modifier,
) {
    val iconSize = 48.dp

    val vehicleType = routeAccents.type.typeText(LocalContext.current, true)

    val headerText =
        when (type) {
            MissingTripCardType.Complete -> stringResource(R.string.trip_complete)
            MissingTripCardType.NotAvailable -> stringResource(R.string.trip_not_available)
        }
    val bodyText =
        when (type) {
            MissingTripCardType.Complete -> stringResource(R.string.trip_complete_body, vehicleType)
            MissingTripCardType.NotAvailable ->
                stringResource(R.string.trip_not_available_body, vehicleType)
        }

    Column(
        modifier = modifier.haloContainer(2.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(iconSize).clip(CircleShape).background(routeAccents.color),
                Alignment.Center,
            ) {
                val painter = routeIcon(routeAccents.type)
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = routeAccents.textColor,
                )
            }
            Text(headerText, Modifier.weight(1f), style = Typography.title2Bold)
        }

        HorizontalDivider(color = routeAccents.color.copy(alpha = 0.25f), thickness = 2.dp)

        Text(bodyText, style = Typography.callout)
    }
}

@Composable
@Preview
fun MissingTripCardCompletePreview() {
    val testData = TestData.clone()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MissingTripCard(MissingTripCardType.Complete, TripRouteAccents(testData.getRoute("Red")))
        MissingTripCard(
            MissingTripCardType.Complete,
            TripRouteAccents(testData.getRoute("Green-C")),
        )
        MissingTripCard(MissingTripCardType.Complete, TripRouteAccents(testData.getRoute("742")))
        MissingTripCard(MissingTripCardType.Complete, TripRouteAccents(testData.getRoute("15")))
        MissingTripCard(
            MissingTripCardType.Complete,
            TripRouteAccents(testData.getRoute("CR-Lowell")),
        )
    }
}

@Composable
@Preview
fun MissingTripCardNotAvailablePreview() {
    val testData = TestData.clone()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MissingTripCard(
            MissingTripCardType.NotAvailable,
            TripRouteAccents(testData.getRoute("Red")),
        )
        MissingTripCard(
            MissingTripCardType.NotAvailable,
            TripRouteAccents(testData.getRoute("Green-C")),
        )
        MissingTripCard(
            MissingTripCardType.NotAvailable,
            TripRouteAccents(testData.getRoute("742")),
        )
        MissingTripCard(MissingTripCardType.NotAvailable, TripRouteAccents(testData.getRoute("15")))
        MissingTripCard(
            MissingTripCardType.NotAvailable,
            TripRouteAccents(testData.getRoute("CR-Lowell")),
        )
    }
}
