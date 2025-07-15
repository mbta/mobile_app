/**
 * Derived from
 * https://android.googlesource.com/platform/frameworks/support/+/065f8f4c9800a64e5344e69e1a0aef65fa981370/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/BottomSheetScaffold.kt,
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

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.R as MaterialR
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** @see androidx.compose.material3.BottomSheetScaffold */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@ExperimentalMaterial3Api
fun BottomSheetScaffold(
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetSmallHeight: Dp = 200.dp,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    // padding the map to account for the sheet height is a bit of a mess, especially because
    // padding with the full layout height crashes the app, so we need a max padding
    var layoutHeight by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val actualSheetHeight =
        with(density) {
            scaffoldState.bottomSheetState.anchoredDraggableState.offset
                .takeUnless { it.isNaN() }
                ?.let { (layoutHeight - it).toDp() }
        }
    val maxPadding = with(density) { layoutHeight.toDp() - sheetSmallHeight }
    BottomSheetScaffoldLayout(
        modifier = modifier.onGloballyPositioned { layoutHeight = it.boundsInWindow().height },
        topBar = topBar,
        body = {
            content(
                PaddingValues(
                    bottom =
                        (actualSheetHeight ?: 0.dp).coerceIn(
                            sheetSmallHeight,
                            max(maxPadding, sheetSmallHeight),
                        )
                )
            )
        },
        snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
        sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
        sheetState = scaffoldState.bottomSheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        bottomSheet = {
            StandardBottomSheet(
                state = scaffoldState.bottomSheetState,
                smallHeight = sheetSmallHeight,
                sheetMaxWidth = sheetMaxWidth,
                sheetSwipeEnabled = sheetSwipeEnabled,
                shape = sheetShape,
                containerColor = sheetContainerColor,
                contentColor = sheetContentColor,
                tonalElevation = sheetTonalElevation,
                shadowElevation = sheetShadowElevation,
                dragHandle = sheetDragHandle,
                content = sheetContent,
            )
        },
    )
}

/** @see androidx.compose.material3.BottomSheetScaffoldState */
@ExperimentalMaterial3Api
@Stable
class BottomSheetScaffoldState(
    val bottomSheetState: SheetState,
    val snackbarHostState: SnackbarHostState,
)

/** @see androidx.compose.material3.rememberBottomSheetScaffoldState */
@Composable
@ExperimentalMaterial3Api
fun rememberBottomSheetScaffoldState(
    bottomSheetState: SheetState = rememberStandardBottomSheetState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): BottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState,
        )
    }
}

/** @see androidx.compose.material3.rememberStandardBottomSheetState */
@Composable
fun rememberStandardBottomSheetState(
    initialValue: SheetValue = SheetValue.Medium,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = true,
) =
    rememberSheetState(
        confirmValueChange = confirmValueChange,
        initialValue = initialValue,
        skipHiddenState = skipHiddenState,
    )

/** @see androidx.compose.material3.StandardBottomSheet */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("PrivateResource")
@Composable
private fun StandardBottomSheet(
    state: SheetState,
    smallHeight: Dp,
    sheetMaxWidth: Dp,
    sheetSwipeEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    dragHandle: @Composable (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val smallHeightPx = with(LocalDensity.current) { smallHeight.toPx() }
    val nestedScroll =
        if (sheetSwipeEnabled) {
            Modifier.nestedScroll(
                remember(state.anchoredDraggableState) {
                    ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = state,
                        orientation = orientation,
                        onFling = { scope.launch { state.settle(it) } },
                    )
                }
            )
        } else {
            Modifier
        }
    Surface(
        modifier =
            Modifier.widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .requiredHeightIn(min = smallHeight)
                .then(nestedScroll)
                .draggableAnchors(state.anchoredDraggableState, orientation) { _, constraints ->
                    val layoutHeight = constraints.maxHeight.toFloat()
                    val newTarget =
                        if (state.currentValue == SheetValue.Hidden) {
                            SheetValue.Hidden
                        } else {
                            state.anchoredDraggableState.targetValue
                        }
                    val newAnchors: DraggableAnchors<SheetValue> = DraggableAnchors {
                        if (newTarget == SheetValue.Hidden) {
                            SheetValue.Hidden at layoutHeight
                        }
                        SheetValue.Small at layoutHeight - smallHeightPx
                        SheetValue.Medium at (layoutHeight * 0.44).toFloat()
                        SheetValue.Large at 0f
                    }

                    return@draggableAnchors newAnchors to newTarget
                }
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = orientation,
                    enabled = sheetSwipeEnabled,
                ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Box(Modifier.fillMaxWidth()) {
            content()
            if (dragHandle != null) {
                var handleVisible by remember { mutableStateOf(true) }
                LaunchedEffect(state.currentValue) {
                    handleVisible = state.currentValue != SheetValue.Large
                }
                val collapseActionLabel =
                    stringResource(MaterialR.string.m3c_bottom_sheet_collapse_description)
                val expandActionLabel =
                    stringResource(MaterialR.string.m3c_bottom_sheet_expand_description)
                Box(
                    Modifier.align(TopCenter).semantics(mergeDescendants = true) {
                        with(state) {
                            // Provides semantics to interact with the bottomsheet if there is more
                            // than one anchor to swipe to and swiping is enabled.
                            if (anchoredDraggableState.anchors.size > 1 && sheetSwipeEnabled) {
                                when (currentValue) {
                                    SheetValue.Hidden -> {}
                                    SheetValue.Small -> {
                                        expand(expandActionLabel) {
                                            scope.launch { animateTo(SheetValue.Medium) }
                                            true
                                        }
                                    }
                                    SheetValue.Medium -> {
                                        collapse(collapseActionLabel) {
                                            scope.launch { animateTo(SheetValue.Small) }
                                            true
                                        }
                                        expand(expandActionLabel) {
                                            scope.launch { animateTo(SheetValue.Large) }
                                            true
                                        }
                                    }
                                    SheetValue.Large -> {
                                        collapse(collapseActionLabel) {
                                            scope.launch { animateTo(SheetValue.Medium) }
                                            true
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    AnimatedVisibility(handleVisible, enter = fadeIn(), exit = fadeOut()) {
                        dragHandle()
                    }
                }
            }
        }
    }
}

/** @see androidx.compose.material3.BottomSheetScaffoldLayout */
@Composable
private fun BottomSheetScaffoldLayout(
    modifier: Modifier,
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sheetOffset: () -> Float,
    sheetState: SheetState,
    containerColor: Color,
    contentColor: Color,
) {
    Layout(
        contents =
            listOf<@Composable () -> Unit>(
                topBar ?: {},
                {
                    Surface(
                        modifier = modifier,
                        color = containerColor,
                        contentColor = contentColor,
                        content = body,
                    )
                },
                bottomSheet,
                snackbarHost,
            )
    ) {
        (topBarMeasurables, bodyMeasurables, bottomSheetMeasurables, snackbarHostMeasurables),
        constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sheetPlaceables = bottomSheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            val sheetWidth = sheetPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val sheetOffsetX = Integer.max(0, (layoutWidth - sheetWidth) / 2)

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    SheetValue.Hidden,
                    SheetValue.Small,
                    SheetValue.Medium -> sheetOffset().roundToInt() - snackbarHeight
                    SheetValue.Large -> layoutHeight - snackbarHeight
                }

            // Placement order is important for elevation
            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(sheetOffsetX, 0) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}
