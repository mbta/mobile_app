package com.mbta.tid.mbta_app.model

actual val FeaturePromo.addedInVersion: AppVersion
    get() = AppVersion(UInt.MAX_VALUE, UInt.MAX_VALUE, UInt.MAX_VALUE)
