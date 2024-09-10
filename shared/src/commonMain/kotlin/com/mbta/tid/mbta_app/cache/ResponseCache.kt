package com.mbta.tid.mbta_app.cache

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Serializable
data class Response(
    val etag: String?,
    @Serializable(with = TimeMarkSerializer::class)
    var fetchTime: TimeSource.Monotonic.ValueTimeMark,
    val data: String
)

class ResponseCache(private val cacheKey: String, val maxAge: Duration = 1.hours) : KoinComponent {
    internal var data: Response? = null
    internal val cacheFile: CacheFile by inject { parametersOf(cacheKey) }
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
            val cachedOnDish = cacheFile.read() ?: return null
            return Json.decodeFromString(cachedOnDish)
        } catch (error: Exception) {
            return null
        }
    }

    private fun writeData(response: Response) {
        try {
            cacheFile.write(Json.encodeToString(response))
        } catch (error: Exception) {
            println("Writing to '$cacheKey' cache file failed. $error")
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
