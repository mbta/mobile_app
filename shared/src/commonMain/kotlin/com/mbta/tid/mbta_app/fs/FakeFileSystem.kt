package com.mbta.tid.mbta_app.fs

internal class FakeFileSystem : FileSystem {
    private val files = mutableMapOf<Path, String>()

    override fun createDirectories(path: Path) {}

    override fun read(file: Path): String = checkNotNull(files[file])

    override fun write(file: Path, text: String) {
        files[file] = text
    }

    override fun delete(path: Path) {
        files.remove(path)
    }
}
