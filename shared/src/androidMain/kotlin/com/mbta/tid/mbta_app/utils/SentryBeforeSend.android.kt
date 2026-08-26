package com.mbta.tid.mbta_app.utils

import io.sentry.kotlin.multiplatform.SentryEvent
import io.sentry.protocol.OperatingSystem

// In shared code we can't access Sentry SDK classes outside of io.sentry.kotlin.multiplatform, so
// in order to cast to the OperatingSystem class we need to implement this in platform-specific code
internal class AndroidSentryBeforeSend : SentryBeforeSend {
    override fun processEvent(event: SentryEvent): SentryEvent? {
        val osContext = event.contexts["os"] as? OperatingSystem
        val isGoogleCI = osContext?.kernelVersion?.contains("android-build") ?: false
        return if (isGoogleCI) null else event
    }
}

internal actual fun getSentryBeforeSend(): SentryBeforeSend = AndroidSentryBeforeSend()
