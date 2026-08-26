package com.mbta.tid.mbta_app

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryEvent
import io.sentry.kotlin.multiplatform.SentryOptions

public fun initializeSentry(dsn: String, environment: String) {
    val configuration: (SentryOptions) -> Unit = {
        it.dsn = dsn
        it.environment = environment
        it.beforeBreadcrumb = { breadcrumb -> breadcrumb }
        it.beforeSend = { event -> if (shouldSkipEvent(event)) null else event }
    }
    Sentry.init(configuration)
}

private fun shouldSkipEvent(event: SentryEvent): Boolean {
    val osContext = event.contexts["os"] as? Map<*, *>
    val kernelVersion = osContext?.get("kernel_version") as? String
    val isGoogleCI = kernelVersion?.contains("android-build") ?: false
    return isGoogleCI
}
