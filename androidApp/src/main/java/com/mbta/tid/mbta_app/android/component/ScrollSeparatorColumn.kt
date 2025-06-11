package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun ScrollSeparatorColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    haloColor: Color = colorResource(R.color.halo),
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable (ColumnScope.() -> Unit),
) {
    Box(Modifier, Alignment.TopCenter) {
        Column(
            Modifier.verticalScroll(scrollState).then(modifier),
            verticalArrangement,
            horizontalAlignment,
            content,
        )
        AnimatedVisibility(scrollState.canScrollBackward, enter = fadeIn(), exit = fadeOut()) {
            HaloSeparator(Modifier.testTag("separator"), haloColor)
        }
    }
}
