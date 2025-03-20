package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.utils.SystemPaths
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.etag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class ResponseMetadata(
    val etag: String?,
    @Serializable(with = TimeMarkSerializer::class)
    var fetchTime: TimeSource.Monotonic.ValueTimeMark,
    val invalidationKey: String? = null
)

@Serializable data class Response<T>(val metadata: ResponseMetadata, val body: T)

class ResponseCache<T : Any>(
    private val cacheKey: String,
    val maxAge: Duration,
    private val serializer: KSerializer<T>,
    // The invalidation key is written to the cache metadata on disk, if a loaded key doesn't match
    // the key provided on cache creation, the data will be reloaded from the backend. This can be
    // set to any arbitrary string value, but should only be changed if you want to wipe the cached
    // data after an update, even when the etag matches the backend.
    private val invalidationKey: String? = null
) : KoinComponent {
    companion object {
        const val CACHE_SUBDIRECTORY = "responseCache"

        inline fun <reified T : Any> create(
            cacheKey: String,
            maxAge: Duration = 1.hours,
            invalidationKey: String? = null
        ) = ResponseCache<T>(cacheKey, maxAge, json.serializersModule.serializer(), invalidationKey)
    }

    internal var data: Response<T>? = null

    private val flow = MutableStateFlow<T?>(null)
    val state = flow.asStateFlow()

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

    private fun decodeString(body: String): T {
        return json.decodeFromString(serializer, body)
    }

    private fun getPossiblyStaleData(): Response<T>? {
        return this.data ?: readData()
    }

    private fun getData(): Response<T>? {
        val data = getPossiblyStaleData()
        if (data != null) {
            flow.value = data.body
        }
        return if (data != null && data.metadata.fetchTime.elapsedNow() < maxAge) {
            data
        } else {
            null
        }
    }

    private suspend fun putData(response: HttpResponse) {
        val responseBody = response.bodyAsText()
        val nextData =
            Response(
                ResponseMetadata(response.etag(), TimeSource.Monotonic.markNow(), invalidationKey),
                decodeString(responseBody)
            )
        this.data = nextData
        this.flow.value = nextData.body

        this.writeData(nextData.metadata, responseBody)
    }

    private fun readData(): Response<T>? {
        try {
            val diskMetadata: ResponseMetadata =
                json.decodeFromString(fileSystem.read(cacheMetadataFilePath) { readUtf8() })
            if (diskMetadata.invalidationKey != invalidationKey) {
                return null
            }
            val diskData = decodeString(fileSystem.read(cacheFilePath) { readUtf8() })
            this.data = Response(diskMetadata, diskData)
            return this.data
        } catch (error: Exception) {
            return null
        }
    }

    private fun writeData(metadata: ResponseMetadata, responseBody: String) {
        try {
            fileSystem.createDirectories(cacheFilePath.parent!!)
            fileSystem.write(cacheFilePath) { writeUtf8(responseBody) }
            writeMetadata(metadata)
        } catch (error: Exception) {
            println("Writing to '$cacheFilePath' failed. $error")
        }
    }

    private fun writeMetadata(metadata: ResponseMetadata) {
        try {
            fileSystem.write(cacheMetadataFilePath) { writeUtf8(json.encodeToString(metadata)) }
        } catch (error: Exception) {
            println("Writing to '${cacheMetadataFilePath}' failed. $error")
        }
    }

    suspend fun getOrFetch(fetch: suspend (String?) -> HttpResponse): ApiResult<T> {
        lock.withLock {
            val cachedData = this.getData()
            if (cachedData != null) {
                return ApiResult.Ok(cachedData.body)
            }

            try {
                val httpResponse = fetch(this.data?.metadata?.etag)
                return when (httpResponse.status) {
                    HttpStatusCode.NotModified -> {
                        val data =
                            this.data
                                ?: return ApiResult.Error(message = "Failed to update cached data")
                        data.metadata.fetchTime = TimeSource.Monotonic.markNow()
                        writeMetadata(data.metadata)
                        ApiResult.Ok(data.body)
                    }
                    HttpStatusCode.OK -> {
                        this.putData(httpResponse)
                        ApiResult.Ok(
                            this.getData()?.body
                                ?: throw RuntimeException("Failed to set cached data")
                        )
                    }
                    else ->
                        ApiResult.Error(
                            code = httpResponse.status.value,
                            message = httpResponse.bodyAsText()
                        )
                }
            } catch (ex: Exception) {
                return ApiResult.Error(message = ex.message ?: ex.toString())
            }
        }
    }
}
