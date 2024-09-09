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
import org.koin.core.component.KoinComponent

data class Response(
    val etag: String?,
    var fetchTime: TimeSource.Monotonic.ValueTimeMark,
    val data: String
)

class ResponseCache(val maxAge: Duration = 1.hours) : KoinComponent {
    internal var data: Response? = null
    private val lock = Mutex()

    private fun getData(): Response? {
        val data = this.data ?: return null
        return if (data.fetchTime.elapsedNow() < maxAge) {
            data
        } else {
            null
        }
    }

    private suspend fun putData(response: HttpResponse) {
        this.data = Response(response.etag(), TimeSource.Monotonic.markNow(), response.bodyAsText())
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
                    val data = this.data ?: throw RuntimeException("Failed to updated cached data")
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
