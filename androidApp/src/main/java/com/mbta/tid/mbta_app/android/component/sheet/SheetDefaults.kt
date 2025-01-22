/**
 * Derived from
 * https://android.googlesource.com/platform/frameworks/support/+/065f8f4c9800a64e5344e69e1a0aef65fa981370/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SheetDefaults.kt,
 * which is
 *
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.mbta.tid.mbta_app.android.component.sheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/** @see androidx.compose.material3.SheetState */
@OptIn(ExperimentalFoundationApi::class)
class SheetState(
    density: Density,
    initialValue: SheetValue = SheetValue.Medium,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) {
    val currentValue: SheetValue
        get() = anchoredDraggableState.currentValue

    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    internal suspend fun animateTo(targetValue: SheetValue) {
        anchoredDraggableState.animateTo(targetValue)
    }

    internal suspend fun settle(velocity: Float) {
        anchoredDraggableState.settle(velocity)
    }

    internal var anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = BottomSheetAnimationSpec,
            decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec(),
            confirmValueChange = confirmValueChange,
        )

    companion object {
        fun Saver(
            confirmValueChange: (SheetValue) -> Boolean,
            density: Density,
        ) =
            Saver<SheetState, SheetValue>(
                save = { it.currentValue },
                restore = { savedValue ->
                    SheetState(
                        density,
                        savedValue,
                        confirmValueChange,
                    )
                }
            )
    }
}

/** @see androidx.compose.material3.SheetValue */
enum class SheetValue {
    Large,
    Medium,
    Small,
    Hidden
}

/** @see androidx.compose.material3.ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection */
@OptIn(ExperimentalFoundationApi::class)
internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: SheetState,
    orientation: Orientation,
    onFling: (velocity: Float) -> Unit
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = sheetState.requireOffset()
            val minAnchor = sheetState.anchoredDraggableState.anchors.minAnchor()
            return if (toFling < 0 && currentOffset > minAnchor) {
                onFling(toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            onFling(available.toFloat())
            return available
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

/** @see androidx.compose.material3.rememberSheetState */
@Composable
internal fun rememberSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = SheetValue.Medium,
    skipHiddenState: Boolean = false,
): SheetState {
    val density = LocalDensity.current
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            SheetState.Saver(
                confirmValueChange = confirmValueChange,
                density = density,
            )
    ) {
        SheetState(
            density,
            initialValue,
            confirmValueChange,
        )
    }
}

/** @see androidx.compose.material3.BottomSheetAnimationSpec */
private val BottomSheetAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
