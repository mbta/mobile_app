package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import io.sentry.kotlin.multiplatform.Scope
import io.sentry.kotlin.multiplatform.Sentry

public interface ISentryRepository {

    public fun captureMessage(msg: String)

    public fun captureMessage(msg: String, additionalDetails: Scope.() -> Unit)

    public fun captureException(throwable: Throwable)
}

internal class SentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        Sentry.captureMessage(msg)
    }

    override fun captureMessage(msg: String, additionalDetails: Scope.() -> Unit) {
        Sentry.captureMessage(msg) { it.additionalDetails() }
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}

public class MockSentryRepository
@DefaultArgumentInterop.Enabled
constructor(
    public var onCaptureMessage: (String) -> Unit = {},
    public var onCaptureMessageWithDetails: (String) -> Unit = {},
    public var onCaptureException: (Throwable) -> Unit = {},
) : ISentryRepository {
    override fun captureMessage(msg: String): Unit = onCaptureMessage(msg)

    override fun captureMessage(msg: String, additionalDetails: Scope.() -> Unit): Unit =
        onCaptureMessageWithDetails(msg)

    override fun captureException(throwable: Throwable): Unit = onCaptureException(throwable)
}
