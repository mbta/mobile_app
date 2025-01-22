package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlinx.datetime.Clock

@Composable
fun LoadingStopDetailsView(stopFilter: StopDetailsFilter?, tripFilter: TripDetailsFilter?) {
    Column {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loadingShimmer()) {
                if (stopFilter == null) {
                    Column(
                        modifier =
                            Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f)
                    ) {
                        val placeholderPatterns = LoadingPlaceholders.patternsByStop()
                        StopDetailsRouteView(
                            LoadingPlaceholders.patternsByStop(),
                            Clock.System.now(),
                            pinned = false,
                            onPin = {},
                            {}
                        )
                    }
                } else {
                    StopDetailsFilteredRouteView(
                        departures = LoadingPlaceholders.stopDetailsDepartures(stopFilter),
                        global = null,
                        now = Clock.System.now(),
                        stopFilter = stopFilter,
                        tripFilter = tripFilter,
                        updateStopFilter = {},
                        openAlertDetails = {}
                    )
                }
            }
        }
    }
}
