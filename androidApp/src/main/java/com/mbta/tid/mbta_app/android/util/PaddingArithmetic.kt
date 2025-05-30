package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

private class ArithmeticPadding(
    private val p1: PaddingValues,
    private val p2: PaddingValues,
    private val math: (x: Dp, y: Dp) -> Dp,
) : PaddingValues {
    override fun calculateBottomPadding(): Dp {
        return math(p1.calculateBottomPadding(), p2.calculateBottomPadding())
    }

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        return math(
            p1.calculateLeftPadding(layoutDirection),
            p2.calculateLeftPadding(layoutDirection),
        )
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        return math(
            p1.calculateRightPadding(layoutDirection),
            p2.calculateRightPadding(layoutDirection),
        )
    }

    override fun calculateTopPadding(): Dp {
        return math(p1.calculateTopPadding(), p2.calculateTopPadding())
    }
}

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    return ArithmeticPadding(this, other) { x, y -> x + y }
}
