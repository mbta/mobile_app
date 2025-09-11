package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.fs.FakeFileSystem
import com.mbta.tid.mbta_app.fs.FileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val fileSystem: FileSystem = FakeFileSystem()
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
