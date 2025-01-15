package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.AppVersion

class CurrentAppVersionRepository(versionName: String) : ICurrentAppVersionRepository {
    private val appVersion = AppVersion.parse(versionName)

    override fun getCurrentAppVersion() = appVersion
}
