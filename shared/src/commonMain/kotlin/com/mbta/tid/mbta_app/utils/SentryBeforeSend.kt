package com.mbta.tid.mbta_app.utils

import io.sentry.kotlin.multiplatform.SentryEvent

internal interface SentryBeforeSend {
    fun processEvent(event: SentryEvent): SentryEvent?
}

internal expect fun getSentryBeforeSend(): SentryBeforeSend
