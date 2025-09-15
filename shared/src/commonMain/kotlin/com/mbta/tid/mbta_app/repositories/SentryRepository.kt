package com.mbta.tid.mbta_app.repositories

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

public class MockSentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        TODO("Not yet implemented")
    }

    override fun captureMessage(msg: String, additionalDetails: Scope.() -> Unit) {
        TODO("Not yet implemented")
    }

    override fun captureException(throwable: Throwable) {
        TODO("Not yet implemented")
    }
}
