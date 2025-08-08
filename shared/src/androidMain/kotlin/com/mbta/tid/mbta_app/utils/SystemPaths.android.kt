package com.mbta.tid.mbta_app.utils

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

internal class AndroidSystemPaths(val context: Context) : SystemPaths {
    override val data: Path
        get() = context.filesDir.absolutePath.toPath()

    override val cache: Path
        get() = context.cacheDir.absolutePath.toPath()
}
