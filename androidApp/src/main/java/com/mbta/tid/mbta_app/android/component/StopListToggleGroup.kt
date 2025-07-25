package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

@Composable
fun StopListToggleGroup(stopsExpanded: Boolean, stopList: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(stopsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
        Column { stopList() }
    }
}
