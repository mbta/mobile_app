package com.mbta.tid.mbta_app.android.retryable

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.AndroidComposeUiTestEnvironment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Utils for marking tests as retryable
 *
 * A mix of the RetryableComposeTestRule from https://github.com/androidx/androidx/pull/574 and the
 * Retry AnnotationClass from
 * https://euryperez.dev/how-to-retry-your-instrumented-tests-in-android-e60581114826
 */
const val DEFAULT_RETRIES = 3

/**
 * An implementation of [ComposeTestRule] that will correctly reset itself in-between test retries.
 *
 * This is necessary because there is currently no ability to reset the
 * [AndroidComposeUiTestEnvironment] in the standard [AndroidComposeTestRule].
 *
 * @param ruleProvider Function to create the underlying ComposeTestRule rule, defaults to
 *   [createEmptyComposeRule]
 */
class RetryableComposeTestRule
constructor(
    val maxRetries: Int = DEFAULT_RETRIES,
    val stopAfterSuccess: Boolean = true,
    private val ruleProvider: () -> ComposeContentTestRule = ::createComposeRule,
) : ComposeContentTestRule {
    private var rule: ComposeContentTestRule = ruleProvider.invoke()

    override val density: Density
        get() = rule.density

    override val mainClock: MainTestClock
        get() = rule.mainClock

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                for (i in 0 until maxRetries) {
                    Log.i("retryableTest", "${description.displayName}: run ${(i + 1)}")
                    try {
                        rule.apply(base, description).evaluate()
                        Log.i(
                            "retryableTest",
                            "${description.displayName}: run ${(i + 1)} succeeded.",
                        )

                        if (stopAfterSuccess) {

                            return
                        }
                    } catch (t: Throwable) {
                        Log.i("retryableTest", "${description.displayName}: run ${(i + 1)} failed.")
                    } finally {
                        rule = ruleProvider.invoke()
                    }
                }

                Log.i(
                    "retryableTest",
                    "${description.displayName}: Giving up after $maxRetries attempts.",
                )
            }
        }
    }

    override suspend fun awaitIdle() = rule.awaitIdle()

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean,
    ): SemanticsNodeInteractionCollection = rule.onAllNodes(matcher, useUnmergedTree)

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean,
    ): SemanticsNodeInteraction = rule.onNode(matcher, useUnmergedTree)

    override fun registerIdlingResource(idlingResource: IdlingResource) =
        rule.registerIdlingResource(idlingResource)

    override fun <T> runOnIdle(action: () -> T): T = rule.runOnIdle(action)

    override fun <T> runOnUiThread(action: () -> T): T = rule.runOnUiThread(action)

    override fun setContent(composable: @Composable () -> Unit) {
        rule.setContent(composable)
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) =
        rule.unregisterIdlingResource(idlingResource)

    override fun waitForIdle() = rule.waitForIdle()

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) =
        rule.waitUntil(timeoutMillis, condition)

    @ExperimentalTestApi
    override fun waitUntilAtLeastOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) {
        rule.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }

    @ExperimentalTestApi
    override fun waitUntilDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long) {
        rule.waitUntilDoesNotExist(matcher, timeoutMillis)
    }

    @ExperimentalTestApi
    override fun waitUntilExactlyOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) {
        rule.waitUntilExactlyOneExists(matcher, timeoutMillis)
    }

    @ExperimentalTestApi
    override fun waitUntilNodeCount(matcher: SemanticsMatcher, count: Int, timeoutMillis: Long) {
        rule.waitUntilNodeCount(matcher, count, timeoutMillis)
    }
}
