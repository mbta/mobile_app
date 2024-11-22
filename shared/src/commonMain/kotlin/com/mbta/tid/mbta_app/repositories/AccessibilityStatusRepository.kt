package com.mbta.tid.mbta_app.repositories

interface IAccessibilityStatusRepository {
    fun isScreenReaderEnabled(): Boolean
}

class MockAccessibilityStatusRepository(private val isScreenReaderEnabled: Boolean) :
    IAccessibilityStatusRepository {
    override fun isScreenReaderEnabled() = isScreenReaderEnabled
}
