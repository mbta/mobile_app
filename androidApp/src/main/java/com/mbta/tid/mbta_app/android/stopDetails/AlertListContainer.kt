package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertCardSpec
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun AlertListContainer(
    highPriority: List<@Composable (modifier: Modifier) -> Unit> = listOf(),
    middleContent: (@Composable (modifier: Modifier) -> Unit)? = null,
    lowPriority: List<@Composable (modifier: Modifier) -> Unit> = listOf(),
) {
    val highPriorityCount = highPriority.size
    val lowPriorityCount = lowPriority.size
    val outerCornerRadius = 8.dp
    val internalCornerRadius = 4.dp
    Column(
        modifier = Modifier.haloContainer(2.dp, backgroundColor = Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        highPriority.forEachIndexed { index, content ->
            val topRadius = if (index == 0) outerCornerRadius else internalCornerRadius
            val bottomRadius =
                if (index == highPriorityCount && (middleContent == null && lowPriorityCount == 0))
                    outerCornerRadius
                else internalCornerRadius
            content(
                Modifier.background(
                    colorResource(R.color.fill3),
                    RoundedCornerShape(
                        topStart = topRadius,
                        topEnd = topRadius,
                        bottomStart = bottomRadius,
                        bottomEnd = bottomRadius,
                    ),
                )
            )
        }

        val middleContentTopRadius =
            if (highPriorityCount == 0) outerCornerRadius else internalCornerRadius
        val middleContentBottomRadius =
            if (lowPriorityCount == 0) outerCornerRadius else internalCornerRadius

        middleContent?.invoke(
            Modifier.background(
                colorResource(R.color.fill3),
                RoundedCornerShape(
                    topStart = middleContentTopRadius,
                    topEnd = middleContentTopRadius,
                    bottomStart = middleContentBottomRadius,
                    bottomEnd = middleContentBottomRadius,
                ),
            )
        )

        lowPriority.forEachIndexed { index, content ->
            val topRadius =
                if (index == 0 && highPriorityCount == 0 && middleContent == null) outerCornerRadius
                else internalCornerRadius
            val bottomRadius =
                if (index == highPriorityCount) outerCornerRadius else internalCornerRadius
            content(
                Modifier.background(
                    colorResource(R.color.fill2),
                    RoundedCornerShape(
                        topStart = topRadius,
                        topEnd = topRadius,
                        bottomStart = bottomRadius,
                        bottomEnd = bottomRadius,
                    ),
                )
            )
        }
    }
}

@Composable
@Preview
fun AlertListContainerPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.background(Color.Blue),
    ) {
        AlertListContainer(
            highPriority =
                listOf({ modifier ->
                    AlertCard(
                        ObjectCollectionBuilder.Single.alert({
                            effect = Alert.Effect.ServiceChange
                        }),
                        null,
                        AlertCardSpec.Basic,
                        routeAccents =
                            TripRouteAccents(
                                color = Color.fromHex("80276C"),
                                textColor = Color.fromHex("FFFFFF"),
                                type = RouteType.COMMUTER_RAIL,
                            ),
                        modifier = modifier,
                        onViewDetails = {},
                    )
                }),
            middleContent = { modifier -> NotAccessibleCard(modifier) },
            lowPriority =
                listOf({ modifier ->
                    AlertCard(
                        ObjectCollectionBuilder.Single.alert({
                            effect = Alert.Effect.ElevatorClosure
                            header =
                                "Ruggles Elevator 848 (Lobby to lower busway side platform) unavailable due to maintenance"
                        }),
                        null,
                        AlertCardSpec.Elevator,
                        routeAccents =
                            TripRouteAccents(
                                color = Color.fromHex("ED8B00"),
                                textColor = Color.fromHex("FFFFFF"),
                                type = RouteType.HEAVY_RAIL,
                            ),
                        modifier = modifier,
                        onViewDetails = {},
                    )
                }),
        )

        AlertListContainer(
            highPriority =
                listOf({ modifier ->
                    AlertCard(
                        ObjectCollectionBuilder.Single.alert({
                            effect = Alert.Effect.ServiceChange
                        }),
                        null,
                        AlertCardSpec.Basic,
                        routeAccents =
                            TripRouteAccents(
                                color = Color.fromHex("80276C"),
                                textColor = Color.fromHex("FFFFFF"),
                                type = RouteType.COMMUTER_RAIL,
                            ),
                        modifier = modifier,
                        onViewDetails = {},
                    )
                })
        )

        AlertListContainer(
            lowPriority =
                listOf({ modifier ->
                    AlertCard(
                        ObjectCollectionBuilder.Single.alert({
                            effect = Alert.Effect.ServiceChange
                        }),
                        null,
                        AlertCardSpec.Basic,
                        routeAccents =
                            TripRouteAccents(
                                color = Color.fromHex("80276C"),
                                textColor = Color.fromHex("FFFFFF"),
                                type = RouteType.COMMUTER_RAIL,
                            ),
                        modifier = modifier,
                        onViewDetails = {},
                    )
                })
        )
    }
}
