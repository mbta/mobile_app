package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.AppVersion

/**
 * The app version is defined at compile time in Android, but the shared library can't see it there,
 * so a simple `expect`/`actual` won't cut it.
 */
fun interface ICurrentAppVersionRepository {
    fun getCurrentAppVersion(): AppVersion?
}

class MockCurrentAppVersionRepository(private val currentAppVersion: AppVersion?) :
    ICurrentAppVersionRepository {
    override fun getCurrentAppVersion() = currentAppVersion
}
