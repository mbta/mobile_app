package com.mbta.tid.mbta_app.fs

internal interface FileSystem {
    fun createDirectories(path: Path)

    fun read(file: Path): String

    fun write(file: Path, text: String)

    fun delete(path: Path)
}
