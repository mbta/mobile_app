package com.mbta.tid.mbta_app.utils

import io.sentry.kotlin.multiplatform.SentryEvent

internal class IOSSentryBeforeSend : SentryBeforeSend {
    override fun processEvent(event: SentryEvent): SentryEvent? = event
}

internal actual fun getSentryBeforeSend(): SentryBeforeSend = IOSSentryBeforeSend()
