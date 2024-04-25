package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.junit4.ComposeContentTestRule

class ManualIdlingResource : IdlingResource {
    override var isIdleNow: Boolean = true
}

inline fun <T> withIdlingResource(resource: ManualIdlingResource, block: () -> T): T {
    resource.isIdleNow = false
    val result = block()
    resource.isIdleNow = true
    return result
}

suspend fun ComposeContentTestRule.awaitIdleIncluding(vararg extraResource: IdlingResource) {
    for (resource in extraResource) {
        registerIdlingResource(resource)
    }
    awaitIdle()
    for (resource in extraResource) {
        unregisterIdlingResource(resource)
    }
}
