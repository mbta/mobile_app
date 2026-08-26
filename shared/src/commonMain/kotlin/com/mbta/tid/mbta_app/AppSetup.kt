package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.utils.getSentryBeforeSend
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

public fun initializeSentry(dsn: String, environment: String) {
    val beforeSend = getSentryBeforeSend()
    val configuration: (SentryOptions) -> Unit = {
        it.dsn = dsn
        it.environment = environment
        it.beforeBreadcrumb = { breadcrumb -> breadcrumb }
        it.beforeSend = beforeSend::processEvent
    }
    Sentry.init(configuration)
}
