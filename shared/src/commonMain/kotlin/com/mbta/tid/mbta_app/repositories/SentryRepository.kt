package com.mbta.tid.mbta_app.repositories

import io.sentry.kotlin.multiplatform.Sentry

interface ISentryRepository {

    fun captureMessage(msg: String)

    fun captureException(throwable: Throwable)
}

class SentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        Sentry.captureMessage(msg)
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}

class MockSentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        TODO("Not yet implemented")
    }

    override fun captureException(throwable: Throwable) {
        TODO("Not yet implemented")
    }
}
