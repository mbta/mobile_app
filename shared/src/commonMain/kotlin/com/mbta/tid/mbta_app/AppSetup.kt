package com.mbta.tid.mbta_app

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.SentryOptions

@OptIn(ExperimentalStdlibApi::class)
public fun initializeSentry(dsn: String, environment: String) {
    val configuration: (SentryOptions) -> Unit = {
        it.dsn = dsn
        it.environment = environment
        it.beforeBreadcrumb = { breadcrumb -> breadcrumb }
        it.beforeSend = { event ->
            println("SENDING EVENT TO SENTRY ${event}, LEVEL: ${event.level}")
            println("SENDING MESSAGE TO SENTRY ${event.message}, LEVEL: ${event.level}")
            event
        }
    }
    Sentry.configureScope { scope -> scope.level = SentryLevel.INFO }
    Sentry.init(configuration)
}
