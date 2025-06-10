package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun ScrollSeparatorLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    haloColor: Color = colorResource(R.color.halo),
    content: LazyListScope.() -> Unit,
) {
    Box(Modifier, Alignment.TopCenter) {
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            flingBehavior = flingBehavior,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            overscrollEffect = overscrollEffect,
            content = content,
        )
        AnimatedVisibility(state.canScrollBackward, enter = fadeIn(), exit = fadeOut()) {
            HaloSeparator(Modifier.testTag("separator"), haloColor)
        }
    }
}
