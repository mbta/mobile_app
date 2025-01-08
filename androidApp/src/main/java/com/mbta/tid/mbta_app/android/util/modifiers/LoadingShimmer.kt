package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.mbta.tid.mbta_app.android.R
import com.valentinilk.shimmer.shimmer

@Composable
fun Modifier.loadingShimmer(): Modifier {
    val contentDesc = stringResource(R.string.loading)
    return this then Modifier.shimmer().clearAndSetSemantics { contentDescription = contentDesc }
}
