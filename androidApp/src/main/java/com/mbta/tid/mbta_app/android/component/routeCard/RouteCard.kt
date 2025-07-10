package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.RouteCardPreviewData
import kotlin.time.Instant
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun RouteCardContainer(
    modifier: Modifier = Modifier,
    data: RouteCardData,
    isFavorite: (FavoriteBridge) -> Boolean,
    onPin: (String) -> Unit,
    showStopHeader: Boolean,
    departureContent: @Composable (RouteCardData.RouteStopData) -> Unit,
) {
    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)
    Column(modifier.haloContainer(1.dp).semantics { testTag = "RouteCard" }) {
        TransitHeader(data.lineOrRoute) { color ->
            if (!enhancedFavorites) {
                PinButton(
                    pinned = isFavorite(FavoriteBridge.Pinned(data.lineOrRoute.id)),
                    color = color,
                ) {
                    onPin(data.lineOrRoute.id)
                }
            }
        }

        data.stopData.forEach {
            if (showStopHeader) {
                StopHeader(it)
            }

            departureContent(it)
        }
    }
}

@Composable
fun RouteCard(
    data: RouteCardData,
    globalData: GlobalResponse?,
    now: Instant,
    isFavorite: (FavoriteBridge) -> Boolean,
    onPin: (String) -> Unit,
    showStopHeader: Boolean,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit,
) {
    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)
    RouteCardContainer(
        data = data,
        isFavorite = isFavorite,
        onPin = onPin,
        showStopHeader = showStopHeader,
    ) {
        Departures(
            it,
            globalData,
            now,
            { routeStopDirection ->
                if (enhancedFavorites) {
                    isFavorite(FavoriteBridge.Favorite(routeStopDirection))
                } else {
                    isFavorite(FavoriteBridge.Pinned(routeStopDirection.route))
                }
            },
        ) { leaf ->
            onOpenStopDetails(it.stop.id, StopDetailsFilter(data.lineOrRoute.id, leaf.directionId))
        }
    }
}

class Previews() {
    val data = RouteCardPreviewData()
    val koin = koinApplication { modules(module { single<Analytics> { MockAnalytics() } }) }

    @Composable
    fun CardForPreview(card: RouteCardData) {
        KoinContext(koin.koin) {
            Box(Modifier.width(358.dp)) {
                RouteCard(
                    card,
                    data.global,
                    data.now,
                    { false },
                    onPin = {},
                    showStopHeader = true,
                    onOpenStopDetails = { _, _ -> },
                )
            }
        }
    }

    @Preview(name = "Downstream disruption", group = "1. Orange Line disruption")
    @Composable
    fun OL1() {
        CardForPreview(data.OL1())
    }

    @Preview(name = "Disrupted stop", group = "1. Orange Line disruption")
    @Composable
    fun OL2() {
        CardForPreview(data.OL2())
    }

    @Preview(
        name = "Show up to the next three trips in the branching direction",
        group = "2. Red Line branching",
    )
    @Composable
    fun RL1() {
        CardForPreview(data.RL1())
    }

    @Preview(name = "Next three trips go to the same destination", group = "2. Red Line branching")
    @Composable
    fun RL2() {
        CardForPreview(data.RL2())
    }

    @Preview(name = "Predictions unavailable for a branch", group = "2. Red Line branching")
    @Composable
    fun RL3() {
        CardForPreview(data.RL3())
    }

    @Preview(name = "Service not running on a branch downstream", group = "2. Red Line branching")
    @Composable
    fun RL4() {
        CardForPreview(data.RL4())
    }

    @Preview(name = "Service disrupted on a branch downstream", group = "2. Red Line branching")
    @Composable
    fun RL5() {
        CardForPreview(data.RL5())
    }

    @Preview(name = "Branching in both directions", group = "3. Green Line branching")
    @Composable
    fun GL1() {
        CardForPreview(data.GL1())
    }

    @Preview(name = "Downstream disruption", group = "3. Green Line branching")
    @Composable
    fun GL2() {
        CardForPreview(data.GL2())
    }

    @Preview(name = "Branching in one direction", group = "4. Silver Line branching")
    @Composable
    fun SL1() {
        CardForPreview(data.SL1())
    }

    @Preview(name = "Branching in one direction", group = "5. CR branching")
    @Composable
    fun CR1() {
        CardForPreview(data.CR1())
    }

    @Preview(
        name = "Next two trips go to the same destination",
        group = "6. Bus route single direction",
    )
    @Composable
    fun Bus1() {
        CardForPreview(data.Bus1())
    }

    @Preview(
        name = "Next two trips go to different destinations",
        group = "6. Bus route single direction",
    )
    @Composable
    fun Bus2() {
        CardForPreview(data.Bus2())
    }

    @Preview(
        name = "Next two trips go to different destinations",
        group = "7. Bus route both directions",
    )
    @Composable
    fun Bus3() {
        CardForPreview(data.Bus3())
    }

    @Preview(name = "Service ended on a branch", group = "8. Service ended")
    @Composable
    fun RL6() {
        CardForPreview(data.RL6())
    }

    @Preview(name = "Service ended on all branches", group = "8. Service ended")
    @Composable
    fun RL7() {
        CardForPreview(data.RL7())
    }

    @Preview(name = "Predictions unavailable on a branch", group = "9. Predictions unavailable")
    @Composable
    fun GL3() {
        CardForPreview(data.GL3())
    }

    @Preview(name = "Predictions unavailable on all branches", group = "9. Predictions unavailable")
    @Composable
    fun GL4() {
        CardForPreview(data.GL4())
    }

    @Preview(name = "Disruption on a branch", group = "A. Disruption")
    @Composable
    fun GL5() {
        CardForPreview(data.GL5())
    }

    @Preview(
        name = "Disruption on a branch, predictions unavailable for other branches",
        group = "A. Disruption",
    )
    @Composable
    fun GL6() {
        CardForPreview(data.GL6())
    }

    @Preview(name = "Disruption on all branches", group = "A. Disruption")
    @Composable
    fun GL7() {
        CardForPreview(data.GL7())
    }
}
