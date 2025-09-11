package com.mbta.tid.mbta_app.fs

internal expect class Path {
    val parent: Path?

    operator fun div(child: String): Path
}

internal expect fun Path(path: String): Path
