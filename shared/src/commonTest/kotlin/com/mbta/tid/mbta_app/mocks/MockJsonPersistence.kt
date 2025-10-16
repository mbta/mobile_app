package com.mbta.tid.mbta_app.mocks

import com.mbta.tid.mbta_app.fs.JsonPersistence
import com.mbta.tid.mbta_app.utils.MockSystemPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.fakefilesystem.FakeFileSystem

internal fun mockJsonPersistence() =
    JsonPersistence(FakeFileSystem(), MockSystemPaths(), Dispatchers.IO)
