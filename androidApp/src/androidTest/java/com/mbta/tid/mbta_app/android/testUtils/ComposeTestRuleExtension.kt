package com.mbta.tid.mbta_app.android.testUtils

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule

const val DEFAULT_TIMEOUT = 2000L

/** Helpers using our default configured timeout to overcome flakiness when tests are run in CI. */
fun ComposeContentTestRule.waitUntilDefaultTimeout(condition: () -> Boolean) {
    this.waitUntil(DEFAULT_TIMEOUT) { condition.invoke() }
}

@ExperimentalTestApi
fun ComposeContentTestRule.waitUntilAtLeastOneExistsDefaultTimeout(matcher: SemanticsMatcher) {
    this.waitUntilAtLeastOneExists(matcher, DEFAULT_TIMEOUT)
}

@ExperimentalTestApi
fun ComposeContentTestRule.waitUntilDoesNotExistDefaultTimeout(matcher: SemanticsMatcher) {
    this.waitUntilDoesNotExist(matcher, DEFAULT_TIMEOUT)
}

@ExperimentalTestApi
fun ComposeContentTestRule.waitUntilExactlyOneExistsDefaultTimeout(matcher: SemanticsMatcher) {
    this.waitUntilExactlyOneExists(matcher, DEFAULT_TIMEOUT)
}

@ExperimentalTestApi
fun ComposeContentTestRule.waitUntilNodeCountDefaultTimeout(matcher: SemanticsMatcher, count: Int) {
    this.waitUntilNodeCount(matcher, count, DEFAULT_TIMEOUT)
}
