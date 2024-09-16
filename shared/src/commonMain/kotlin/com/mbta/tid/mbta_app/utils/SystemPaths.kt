package com.mbta.tid.mbta_app.utils

import okio.Path
import okio.Path.Companion.toPath

interface SystemPaths {
    val data: Path
    val cache: Path
}

class MockSystemPaths(override val data: Path, override val cache: Path) : SystemPaths {
    constructor(data: String, cache: String) : this(data.toPath(), cache.toPath())
}
