package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.utils.SystemPaths
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.etag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class Response(
    val etag: String?,
    @Serializable(with = TimeMarkSerializer::class)
    var fetchTime: TimeSource.Monotonic.ValueTimeMark,
    val data: String
)

class ResponseCache(private val cacheKey: String, val maxAge: Duration = 1.hours) : KoinComponent {
    companion object {
        const val CACHE_SUBDIRECTORY = "responseCache"
    }

    internal var data: Response? = null

    private val systemPaths: SystemPaths by inject()
    private val fileSystem: FileSystem by inject()
    private val cacheFilePath: Path
        get() {
            return systemPaths.cache / CACHE_SUBDIRECTORY / "$cacheKey.json"
        }

    private val lock = Mutex()

    private fun getData(): Response? {
        val data = this.data ?: readData() ?: return null
        return if (data.fetchTime.elapsedNow() < maxAge) {
            data
        } else {
            null
        }
    }

    private suspend fun putData(response: HttpResponse) {
        val nextResponse =
            Response(response.etag(), TimeSource.Monotonic.markNow(), response.bodyAsText())
        this.data = nextResponse
        this.writeData(nextResponse)
    }

    private fun readData(): Response? {
        try {
            val diskData = fileSystem.read(cacheFilePath) { readUtf8() }
            return Json.decodeFromString(diskData)
        } catch (error: Exception) {
            return null
        }
    }

    private fun writeData(response: Response) {
        try {
            fileSystem.createDirectories(cacheFilePath.parent!!)
            fileSystem.write(cacheFilePath) { writeUtf8(Json.encodeToString(response)) }
        } catch (error: Exception) {
            println("Writing to '$cacheFilePath' failed. $error")
        }
    }

    suspend fun getOrFetch(fetch: suspend (String?) -> HttpResponse): String {
        lock.withLock {
            val cachedData = this.getData()
            if (cachedData != null) {
                return cachedData.data
            }

            val httpResponse = fetch(this.data?.etag)
            return when (httpResponse.status) {
                HttpStatusCode.NotModified -> {
                    val data = this.data ?: throw RuntimeException("Failed to update cached data")
                    data.fetchTime = TimeSource.Monotonic.markNow()
                    writeData(data)
                    data.data
                }
                HttpStatusCode.OK -> {
                    this.putData(httpResponse)
                    this.getData()?.data ?: throw RuntimeException("Failed to set cached data")
                }
                else -> throw ResponseException(httpResponse, "Failed to load global data")
            }
        }
    }
}
