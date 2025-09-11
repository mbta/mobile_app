package com.mbta.tid.mbta_app.fs

internal actual class Path(private val segments: List<String>) {
    actual val parent: Path?
        get() = Path(segments.dropLast(1))

    actual operator fun div(child: String) = Path(this.segments + child)
}

internal actual fun Path(path: String) = Path(path.split("/"))
