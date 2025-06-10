package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Bus
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.CommuterRail
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Ferry
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Root
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Silver

val RoutePickerPath.backgroundColor
    @Composable
    get() =
        when (this) {
            is Root -> colorResource(R.color.fill2)
            is Bus -> colorResource(R.color.mode_bus)
            is Silver -> colorResource(R.color.mode_silver)
            is CommuterRail -> colorResource(R.color.mode_commuter_rail)
            is Ferry -> colorResource(R.color.mode_ferry)
        }

val RoutePickerPath.textColor
    @Composable
    get() =
        when (this) {
            is Root -> colorResource(R.color.text)
            is Bus -> Color.fromHex("000000")
            else -> Color.fromHex("FFFFFF")
        }

val RoutePickerPath.haloColor
    @Composable
    get() =
        when (this) {
            is Root -> colorResource(R.color.halo)
            // Halos are drawn over static route colors, they should not be responsive to theme
            is Bus -> colorResource(R.color.halo_light)
            else -> colorResource(R.color.halo_dark)
        }

val RoutePickerPath.Label
    @Composable
    get() =
        when (this) {
            is Root -> {}
            is Bus ->
                Text(
                    stringResource(R.string.bus),
                    style = Typography.headlineBold,
                    color = textColor,
                )
            is Silver ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.silver_line),
                        style = Typography.headlineBold,
                        color = textColor,
                    )
                    Text(
                        stringResource(R.string.bus),
                        style = Typography.headline,
                        color = textColor,
                    )
                }
            is CommuterRail ->
                Text(
                    stringResource(R.string.commuter_rail),
                    style = Typography.headlineBold,
                    color = textColor,
                )
            is Ferry ->
                Text(
                    stringResource(R.string.ferry),
                    style = Typography.headlineBold,
                    color = textColor,
                )
        }
