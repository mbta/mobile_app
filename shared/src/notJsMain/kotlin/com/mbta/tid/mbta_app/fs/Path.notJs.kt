package com.mbta.tid.mbta_app.fs

import okio.Path.Companion.toPath

internal actual typealias Path = okio.Path

internal actual fun Path(path: String) = path.toPath()
