package com.mbta.tid.mbta_app.dependencyInjection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
