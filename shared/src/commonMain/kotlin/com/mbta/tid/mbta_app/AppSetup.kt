package com.mbta.tid.mbta_app

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

public fun initializeSentry(dsn: String, environment: String) {
    val configuration: (SentryOptions) -> Unit = {
        it.dsn = dsn
        it.environment = environment
    }
    Sentry.init(configuration)
}
