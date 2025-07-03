package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.valentinilk.shimmer.shimmer

@Composable
fun LoadingResultsView() {
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        StopResultsView(LoadingPlaceholders.stopResults(), handleSearch = {}, Modifier.shimmer())
    }
}
