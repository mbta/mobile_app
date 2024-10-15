package com.mbta.tid.mbta_app.repositories

import io.sentry.kotlin.multiplatform.Sentry

interface ISentryRepository {

    fun captureMessage(msg: String)
}

class SentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        Sentry.captureMessage(msg)
    }
}

class MockSentryRepository : ISentryRepository {
    override fun captureMessage(msg: String) {
        TODO("Not yet implemented")
    }
}
