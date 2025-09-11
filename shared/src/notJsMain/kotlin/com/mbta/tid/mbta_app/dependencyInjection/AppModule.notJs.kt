package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.fs.FileSystem
import com.mbta.tid.mbta_app.fs.RealFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.SYSTEM

internal actual val fileSystem: FileSystem = RealFileSystem(okio.FileSystem.SYSTEM)
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
