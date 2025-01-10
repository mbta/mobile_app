package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eygraber.compose.placeholder.material3.placeholder
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents

@Composable
fun Modifier.placeholderIfLoading(): Modifier {
    return if (IsLoadingSheetContents.current) {
        placeholder(visible = true)
    } else {
        this
    }
}
