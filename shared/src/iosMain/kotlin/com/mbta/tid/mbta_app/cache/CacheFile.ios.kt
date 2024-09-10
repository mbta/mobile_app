package com.mbta.tid.mbta_app.cache

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocalDomainMask
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL

class IOSCacheFile(override val cacheKey: String) : CacheFile {
    @OptIn(ExperimentalForeignApi::class)
    val cacheFileURL: NSURL
        get() {
            val cacheDirectory: NSURL =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSCachesDirectory,
                    inDomain = NSLocalDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                )
                    ?: throw RuntimeException("Failed to load cache directory")
            return NSURL(string = "backendCache/$cacheKey", relativeToURL = cacheDirectory)
        }

    @OptIn(BetaInteropApi::class)
    override fun write(data: String): Boolean {
        return try {
            NSString.create(string = data).writeToURL(cacheFileURL, true)
        } catch (error: Exception) {
            NSLog("Failed to write data to file for '$cacheKey' cache. $error")
            false
        }
    }

    override fun read(): String? {
        return try {
            NSString.stringWithContentsOfURL(cacheFileURL).toString()
        } catch (error: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun delete(): Boolean {
        return try {
            NSFileManager.defaultManager.removeItemAtURL(cacheFileURL, null)
        } catch (error: Exception) {
            NSLog("Failed to delete file for '$cacheKey' cache. $error")
            false
        }
    }
}

fun createCacheFile(cacheKey: String): CacheFile = IOSCacheFile(cacheKey)
