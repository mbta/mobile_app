package com.mbta.tid.mbta_app.android

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.network.MockPhoenixSocket
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.fail
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun hasClickActionLabel(expected: String?) =
    SemanticsMatcher("has click action label $expected") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == expected
    }

fun hasTextMatching(regex: Regex): SemanticsMatcher {
    val propertyName = "${SemanticsProperties.Text.name} + ${SemanticsProperties.EditableText.name}"
    return SemanticsMatcher("$propertyName matches $regex") {
        val isInEditableTextValue =
            it.config.getOrNull(SemanticsProperties.EditableText)?.text?.matches(regex) ?: false
        val isInTextValue =
            it.config.getOrNull(SemanticsProperties.Text)?.any { item -> item.text.matches(regex) }
                ?: false
        isInEditableTextValue || isInTextValue
    }
}

fun hasRole(role: Role) =
    SemanticsMatcher("has role $role") { node ->
        node.config.getOrNull(SemanticsProperties.Role) == role
    }

@OptIn(ExperimentalStdlibApi::class)
private fun colorToHex(color: Color): String {
    fun valToHex(value: Float): String {
        val byteValue = (value * 255).toInt().toByte()
        return byteValue.toHexString(HexFormat.UpperCase)
    }
    return "#${valToHex(color.red)}${valToHex(color.green)}${valToHex(color.blue)}"
}

private fun Color.isGrey(): Boolean =
    abs(this.red - this.green) < 0.01 && abs(this.green - this.blue) < 0.01

/**
 * It's difficult to determine if a view hierarchy contains the correct image when the image is
 * semantically invisible, but it's easy to capture a screenshot of the view and check it pixel by
 * pixel.
 */
fun SemanticsNodeInteraction.assertHasColor(targetColor: Color) {
    val pixelMap = this.captureToImage().toPixelMap()
    val pixelCounts = mutableMapOf<Color, Int>()
    for (x in 0.rangeUntil(pixelMap.width)) {
        for (y in 0.rangeUntil(pixelMap.height)) {
            val colorHere = pixelMap[x, y]
            if (colorHere == targetColor) {
                return
            }
            pixelCounts[colorHere] = pixelCounts.getOrDefault(colorHere, 0) + 1
        }
    }
    // never returned so did not find the target. hopefully the color that is present instead of the
    // target is either the most common non-grey color or among the most common grey colors (the
    // most common grey will likely be a background, though)
    val legibleResult =
        pixelCounts.entries
            .groupBy(
                { if (it.key.isGrey()) "grey" else "not grey" },
                { colorToHex(it.key) to it.value },
            )
            .mapValues { it.value.sortedByDescending { it.second } }
            .toSortedMap(
                compareBy {
                    when (it) {
                        "not grey" -> 1
                        "grey" -> 2
                        else -> 3
                    }
                }
            )

    fail("Did not contain specified color, but did have $legibleResult")
}

/**
 * Builds a KoinApplication for use in tests, based on the given [objects] by default but with
 * whatever overridden repositories are provided.
 */
fun testKoinApplication(
    objects: ObjectCollectionBuilder = ObjectCollectionBuilder(),
    analytics: Analytics = MockAnalytics(),
    socket: PhoenixSocket = MockPhoenixSocket(),
    clock: Clock = Clock.System,
    repositoryOverrides: MockRepositories.() -> Unit = {},
) = koinApplication {
    modules(
        module {
            single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) { Dispatchers.Default }
            single<CoroutineDispatcher>(named("coroutineDispatcherIO")) { Dispatchers.IO }

            single<Analytics> { analytics }
            single<PhoenixSocket> { socket }
            single<Clock> { clock }
        },
        repositoriesModule(
            MockRepositories().apply {
                useObjects(objects)
                repositoryOverrides()
            }
        ),
        viewModelModule(),
        MainApplication.koinViewModelModule(),
    )
}
