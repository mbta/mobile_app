package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DragDirection {
    LEFT,
    RIGHT,
}

@Composable
fun Modifier.dragAction(
    dragDirection: DragDirection,
    dragWidth: Dp,
    enabled: Boolean = true,
    action: () -> Unit,
): Modifier {
    val localDensity = LocalDensity.current
    val min =
        when (dragDirection) {
            DragDirection.LEFT -> -dragWidth
            DragDirection.RIGHT -> 0.dp
        }
    val max =
        when (dragDirection) {
            DragDirection.LEFT -> 0.dp
            DragDirection.RIGHT -> dragWidth
        }
    val (minPx, maxPx) = with(localDensity) { min.toPx() to max.toPx() }
    var lockDragPosition by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var offsetPosition by remember { mutableFloatStateOf(0f) }
    val animatedOffset by
        animateFloatAsState(if (isDragging || lockDragPosition) offsetPosition else 0f)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(animatedOffset) {
        if (!isDragging) {
            offsetPosition = animatedOffset
        }
    }

    LaunchedEffect(isDragging) {
        val draggedToThreshold =
            offsetPosition ==
                when (dragDirection) {
                    DragDirection.LEFT -> minPx
                    DragDirection.RIGHT -> maxPx
                }
        if (!isDragging && draggedToThreshold) {
            lockDragPosition = true
            action()
            coroutineScope.launch {
                delay(1.seconds)
                lockDragPosition = false
            }
        }
    }
    return offset {
            IntOffset(
                (if (isDragging || lockDragPosition) offsetPosition else animatedOffset)
                    .roundToInt(),
                0,
            )
        }
        .draggable(
            orientation = Orientation.Horizontal,
            enabled = enabled,
            state =
                rememberDraggableState { delta ->
                    if (lockDragPosition) return@rememberDraggableState
                    val newValue = offsetPosition + delta
                    offsetPosition = newValue.coerceIn(minPx, maxPx)
                },
            onDragStarted = { isDragging = true },
            onDragStopped = { isDragging = false },
        )
}
