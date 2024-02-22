package com.mbta.tid.mbta_app

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

fun initializeSentry(dsn: String) {
    val configuration: (SentryOptions) -> Unit = { it.dsn = dsn }
    Sentry.init(configuration)
}