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
data class ResponseMetadata(
    val etag: String?,
    @Serializable(with = TimeMarkSerializer::class)
    var fetchTime: TimeSource.Monotonic.ValueTimeMark
)

@Serializable data class Response(val metadata: ResponseMetadata, val body: String)

class ResponseCache(private val cacheKey: String, val maxAge: Duration = 1.hours) : KoinComponent {
    companion object {
        const val CACHE_SUBDIRECTORY = "responseCache"
    }

    internal var data: Response? = null

    private val fileSystem: FileSystem by inject()
    private val systemPaths: SystemPaths by inject()
    private val cacheDirectory: Path
        get() {
            return systemPaths.cache / CACHE_SUBDIRECTORY
        }

    private val cacheFilePath: Path
        get() {
            return cacheDirectory / "$cacheKey.json"
        }

    private val cacheMetadataFilePath: Path
        get() {
            return cacheDirectory / "$cacheKey-meta.json"
        }

    private val lock = Mutex()

    private fun getData(): Response? {
        val data = this.data ?: readData() ?: return null
        return if (data.metadata.fetchTime.elapsedNow() < maxAge) {
            data
        } else {
            null
        }
    }

    private suspend fun putData(response: HttpResponse) {
        val nextData =
            Response(
                ResponseMetadata(response.etag(), TimeSource.Monotonic.markNow()),
                response.bodyAsText()
            )
        this.data = nextData
        this.writeData(nextData)
    }

    private fun readData(): Response? {
        try {
            val diskMetadata: ResponseMetadata =
                Json.decodeFromString(fileSystem.read(cacheMetadataFilePath) { readUtf8() })
            val diskData = fileSystem.read(cacheFilePath) { readUtf8() }
            this.data = Response(diskMetadata, diskData)
            return this.data
        } catch (error: Exception) {
            return null
        }
    }

    private fun writeData(response: Response) {
        try {
            fileSystem.createDirectories(cacheFilePath.parent!!)
            fileSystem.write(cacheFilePath) { writeUtf8(response.body) }
            writeMetadata(response.metadata)
        } catch (error: Exception) {
            println("Writing to '$cacheFilePath' failed. $error")
        }
    }

    private fun writeMetadata(metadata: ResponseMetadata) {
        try {
            fileSystem.write(cacheMetadataFilePath) { writeUtf8(Json.encodeToString(metadata)) }
        } catch (error: Exception) {
            println("Writing to '${cacheMetadataFilePath}' failed. $error")
        }
    }

    suspend fun getOrFetch(fetch: suspend (String?) -> HttpResponse): String {
        lock.withLock {
            val cachedData = this.getData()
            if (cachedData != null) {
                return cachedData.body
            }

            val httpResponse = fetch(this.data?.metadata?.etag)
            return when (httpResponse.status) {
                HttpStatusCode.NotModified -> {
                    val data = this.data ?: throw RuntimeException("Failed to update cached data")
                    data.metadata.fetchTime = TimeSource.Monotonic.markNow()
                    writeMetadata(data.metadata)
                    data.body
                }
                HttpStatusCode.OK -> {
                    this.putData(httpResponse)
                    this.getData()?.body ?: throw RuntimeException("Failed to set cached data")
                }
                else -> throw ResponseException(httpResponse, "Failed to load global data")
            }
        }
    }
}
