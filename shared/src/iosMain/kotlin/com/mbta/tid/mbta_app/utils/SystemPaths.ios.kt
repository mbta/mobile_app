package com.mbta.tid.mbta_app.utils

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal class IOSSystemPaths() : SystemPaths {
    override val data: Path
        get() =
            getURL(NSApplicationSupportDirectory)?.path()?.toPath()
                ?: throw RuntimeException("Failed to load data directory")

    override val cache: Path
        get() =
            getURL(NSCachesDirectory)?.path()?.toPath()
                ?: throw RuntimeException("Failed to load cache directory")

    private fun getURL(directory: NSSearchPathDirectory): NSURL? {
        return NSFileManager.defaultManager.URLForDirectory(
            directory = directory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    }
}
