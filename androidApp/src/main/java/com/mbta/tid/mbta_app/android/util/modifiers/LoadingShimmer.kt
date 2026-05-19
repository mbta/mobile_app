package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer

@Composable
fun Modifier.loadingShimmer(withShimmer: Boolean = true): Modifier {
    val contentDesc = stringResource(R.string.loading)

    return if (withShimmer) {
        this then Modifier.shimmer().clearAndSetSemantics { contentDescription = contentDesc }
    } else {
        val noShimmer =
            rememberShimmer(
                shimmerBounds = ShimmerBounds.View,
                theme =
                    defaultShimmerTheme.copy(
                        animationSpec = snap(),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Dst,
                        rotation = 0F,
                        shaderColors =
                            listOf(
                                colorResource(R.color.deemphasized),
                                colorResource(R.color.deemphasized),
                            ),
                        shaderColorStops = listOf(0F, 1F),
                        shimmerWidth = 0.dp,
                    ),
            )

        this then
            Modifier.shimmer(customShimmer = noShimmer).clearAndSetSemantics {
                contentDescription = contentDesc
            }
    }
}
