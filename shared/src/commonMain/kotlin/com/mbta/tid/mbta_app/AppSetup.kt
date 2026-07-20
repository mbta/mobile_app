package com.mbta.tid.mbta_app

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.SentryOptions

public fun initializeSentry(dsn: String, environment: String) {
    val configuration: (SentryOptions) -> Unit = {
        it.dsn = dsn
        it.environment = environment
        it.beforeBreadcrumb = { breadcrumb -> breadcrumb }
        it.debug = true
        it.diagnosticLevel = SentryLevel.DEBUG
        it.logs.enabled = true
    }
    Sentry.init(configuration)
}
