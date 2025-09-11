package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.fs.Path

internal interface SystemPaths {
    val data: Path
    val cache: Path
}

internal class MockSystemPaths(override val data: Path, override val cache: Path) : SystemPaths {
    constructor(data: String, cache: String) : this(Path(data), Path(cache))
}
