package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.cubicTo
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.lineTo
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.moveTo
import com.mbta.tid.mbta_app.android.util.rect
import com.mbta.tid.mbta_app.shape.FavoriteStarShape
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
fun StarButton(starred: Boolean?, color: Color, action: () -> Unit) {
    val onClickLabel =
        if (starred == true) {
            stringResource(R.string.remove_favorite)
        } else {
            stringResource(R.string.add_favorite_label)
        }
    IconToggleButton(
        checked = starred == true,
        onCheckedChange = { action() },
        modifier =
            Modifier.size(30.dp).clickable(onClickLabel = onClickLabel, onClick = { action() }),
    ) {
        StarIcon(starred, color)
    }
}

private val easeIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val easeOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val stepAtEnd = Easing { if (it < 1.0f) 0f else 1f }

private fun <T> starringKeyframes(config: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit) =
    keyframes {
        durationMillis = 450
        config()
    }

fun <T> KeyframesSpec.KeyframesSpecConfig<T>.starringStart(value: T) = value at 0 using easeOut

fun <T> KeyframesSpec.KeyframesSpecConfig<T>.starringPeak(
    value: T
): KeyframesSpec.KeyframeEntity<T> {
    value at 200
    return value at 250 using easeIn
}

fun <T> KeyframesSpec.KeyframesSpecConfig<T>.starringEnd(value: T) = value at 450

private val unstarringDuration = 200

private fun <T> unstarring(easing: Easing = easeIn): TweenSpec<T> =
    tween(durationMillis = unstarringDuration, easing = easing)

@Composable
fun StarIcon(
    // starred = null means not starred but still loading so skip animation afterwards
    starred: Boolean?,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.star_route),
    size: Dp = 20.dp,
) {
    val deemphasized = colorResource(R.color.deemphasized)
    val transition = updateTransition(starred, label = "starred")
    val strokeColor by
        transition.animateColor(
            transitionSpec = {
                when {
                    initialState == null -> snap()
                    false isTransitioningTo true ->
                        starringKeyframes {
                            starringStart(color)
                            starringPeak(color.copy(alpha = color.alpha * 0.33f))
                            starringEnd(deemphasized.copy(alpha = 0.1f))
                        }
                    else -> unstarring()
                }
            }
        ) { state ->
            if (state == true) deemphasized.copy(alpha = 0.1f) else color
        }
    val outerFillColor by
        transition.animateColor(
            transitionSpec = {
                when {
                    initialState == null -> snap()
                    false isTransitioningTo true ->
                        starringKeyframes {
                            starringStart(Color.Transparent) using stepAtEnd
                            starringPeak(color)
                        }
                    else -> unstarring(stepAtEnd)
                }
            }
        ) { state ->
            if (state == true) color else Color.Transparent
        }
    val innerSize by
        transition.animateFloat(
            transitionSpec = {
                when {
                    initialState == null -> snap()
                    false isTransitioningTo true ->
                        starringKeyframes {
                            starringStart(0f)
                            starringPeak(1f) using stepAtEnd
                        }
                    else ->
                        keyframes {
                            durationMillis = unstarringDuration
                            0f at 0 using easeIn
                            1f at unstarringDuration using stepAtEnd
                        }
                }
            }
        ) { state ->
            if (state == true) 0f else 0f
        }
    val strokeWidth by
        transition.animateFloat(
            transitionSpec = {
                when {
                    initialState == null -> snap()
                    false isTransitioningTo true ->
                        starringKeyframes {
                            starringStart(2f)
                            starringPeak(10f)
                            starringEnd(2f)
                        }
                    else -> unstarring()
                }
            }
        ) { state ->
            if (state == true) 2f else 2f
        }
    val strokeProportionBehindFill by
        transition.animateFloat(
            transitionSpec = {
                when {
                    initialState == null -> snap()
                    false isTransitioningTo true ->
                        starringKeyframes {
                            starringStart(1f)
                            starringPeak(1f)
                            starringEnd(0f)
                        }
                    else -> unstarring(stepAtEnd)
                }
            }
        ) { state ->
            if (state == true) 0f else 1f
        }
    Box(
        modifier
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .drawWithCache {
                onDrawBehind {
                    val outerShape = FavoriteStarShape(rect)
                    val innerShape = FavoriteStarShape(rect, innerSize)
                    val outerPath =
                        Path().apply {
                            moveTo(outerShape.arms.first().`in`)
                            for (arm in outerShape.arms) {
                                lineTo(arm.`in`)
                                lineTo(arm.outStart)
                                cubicTo(arm.outStartControl, arm.outMidStartControl, arm.outMid)
                                cubicTo(arm.outMidEndControl, arm.outEndControl, arm.outEnd)
                            }
                            close()
                        }
                    val innerPath =
                        Path().apply {
                            moveTo(innerShape.arms.first().`in`)
                            for (arm in innerShape.arms) {
                                lineTo(arm.`in`)
                                lineTo(arm.outStart)
                                cubicTo(arm.outStartControl, arm.outMidStartControl, arm.outMid)
                                cubicTo(arm.outMidEndControl, arm.outEndControl, arm.outEnd)
                            }
                            close()
                        }
                    if (strokeProportionBehindFill > 0f) {
                        drawPath(
                            outerPath,
                            strokeColor.copy(
                                alpha = strokeColor.alpha * strokeProportionBehindFill
                            ),
                            style =
                                Stroke(
                                    width =
                                        strokeWidth * min(this.size.width, this.size.height) / 30,
                                    join = StrokeJoin.Round,
                                ),
                        )
                    }
                    drawPath(outerPath - innerPath, outerFillColor, style = Fill)
                    if (starred == true && innerSize > 0f) {
                        // starring, draw inner as separate path
                        drawPath(innerPath, color, style = Fill)
                    }
                    if (strokeProportionBehindFill < 1f) {
                        drawPath(
                            outerPath,
                            strokeColor.copy(
                                alpha = strokeColor.alpha * (1 - strokeProportionBehindFill)
                            ),
                            style =
                                Stroke(
                                    width =
                                        strokeWidth * min(this.size.width, this.size.height) / 30,
                                    join = StrokeJoin.Round,
                                ),
                        )
                    }
                }
            }
            .requiredSize(size)
            .placeholderIfLoading()
    )
}

@Preview
@Composable
private fun StarIconAnimationPreview() {
    var starred by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            starred = !starred
            delay(0.5.seconds)
        }
    }
    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill1)).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StarIcon(starred, Color.fromHex("FFC72C"), size = 120.dp)
        }
    }
}
