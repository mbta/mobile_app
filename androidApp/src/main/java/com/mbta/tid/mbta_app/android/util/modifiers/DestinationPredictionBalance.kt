package com.mbta.tid.mbta_app.android.util.modifiers

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The behavior that we’d really like is for both the destination and the prediction to get their
 * full intrinsic max width if that can be done with no text wrapping, and to only apply these
 * weights if the text won’t just all fit on one line. Unfortunately, it’s not clear how to do this,
 * but we can get reasonably close by giving the predictions the full width they want as long as
 * they don’t contain arbitrary or long fixed text.
 *
 * One day, Kotlin context parameters will make a lot of this less ugly, because we can just declare
 * `context(rowScope: RowScope) fun Modifier.destinationWidth()`.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
class DestinationPredictionBalance(private val rowScope: RowScope) {
    fun destinationWidth() = with(rowScope) { Modifier.weight(1f).padding(end = 8.dp) }

    fun predictionWidth(containsWrappableText: Boolean) =
        with(rowScope) {
            if (containsWrappableText) Modifier.weight(0.5f) else Modifier.width(IntrinsicSize.Max)
        }
}

/**
 * Constructs a [DestinationPredictionBalance] that refers to the current scope.
 *
 * I wanted to make [DestinationPredictionBalance] a top-level `object`, but then the [RowScope]
 * would’ve had to be a parameter to each method separately, and that’s no good.
 */
val RowScope.DestinationPredictionBalance: DestinationPredictionBalance
    get() = DestinationPredictionBalance(this)
