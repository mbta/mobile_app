package com.mbta.tid.mbta_app.repositories

public interface IAccessibilityStatusRepository {
    public fun isScreenReaderEnabled(): Boolean
}

internal class MockAccessibilityStatusRepository(private val isScreenReaderEnabled: Boolean) :
    IAccessibilityStatusRepository {
    override fun isScreenReaderEnabled() = isScreenReaderEnabled
}
