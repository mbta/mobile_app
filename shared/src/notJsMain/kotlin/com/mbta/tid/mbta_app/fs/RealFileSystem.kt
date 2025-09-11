package com.mbta.tid.mbta_app.fs

internal class RealFileSystem(private val inner: okio.FileSystem) : FileSystem {
    override fun createDirectories(path: Path) {
        inner.createDirectories(path)
    }

    override fun read(file: Path): String = inner.read(file) { readUtf8() }

    override fun write(file: Path, text: String) {
        inner.write(file) { writeUtf8(text) }
    }

    override fun delete(path: Path) {
        inner.delete(path)
    }
}
