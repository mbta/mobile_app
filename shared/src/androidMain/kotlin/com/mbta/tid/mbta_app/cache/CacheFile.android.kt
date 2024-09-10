package com.mbta.tid.mbta_app.cache

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class AndroidCacheFile(override val cacheKey: String, val producePath: () -> String) : CacheFile {
    private val cacheFileName
        get() = "$cacheKey.json"

    private val cacheDirectory = Path(producePath(), "backendCache")
    private val cacheFile by lazy { File(cacheDirectory.toString(), cacheFileName) }

    override fun write(data: String): Boolean {
        try {
            cacheDirectory.createDirectories()
            cacheFile.createNewFile()
            cacheFile.writeText(data)
            return true
        } catch (error: Exception) {
            Log.w("Cache", "Failed to write data to file for '$cacheKey' cache.", error)
            return false
        }
    }

    override fun read(): String? {
        try {
            val text = cacheFile.readText()
            return text
        } catch (error: Exception) {
            return null
        }
    }

    override fun delete(): Boolean {
        return try {
            cacheFile.delete()
        } catch (error: Exception) {
            Log.w("Cache", "Failed to delete file for '$cacheKey' cache.", error)
            false
        }
    }
}

fun createCacheFile(cacheKey: String, context: Context): CacheFile =
    AndroidCacheFile(cacheKey) { context.cacheDir.absolutePath }
