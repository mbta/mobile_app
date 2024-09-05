package com.mbta.tid.mbta_app.cache

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.etag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent

class ResponseCache(val maxAge: Duration = 1.hours) : KoinComponent {
    internal var data: HttpResponse? = null
    internal var dataTimestamp: TimeSource.Monotonic.ValueTimeMark? = null
    private val lock = Mutex()

    private fun getData(): HttpResponse? {
        val data = this.data ?: return null
        val dataTimestamp = this.dataTimestamp ?: return null
        return if (dataTimestamp.elapsedNow() < maxAge) {
            data
        } else {
            null
        }
    }

    private fun putData(response: HttpResponse) {
        this.data = response
        this.dataTimestamp = TimeSource.Monotonic.markNow()
    }

    suspend fun getOrFetch(fetch: suspend (String?) -> HttpResponse): HttpResponse {
        lock.withLock {
            val cachedData = this.getData()
            if (cachedData != null) {
                return cachedData
            }

            val response = fetch(this.data?.etag())
            return when (response.status) {
                HttpStatusCode.NotModified -> {
                    this.dataTimestamp = TimeSource.Monotonic.markNow()
                    this.getData()!!
                }
                HttpStatusCode.OK -> {
                    this.putData(response)
                    response
                }
                else -> throw ResponseException(response, "Failed to load global data")
            }
        }
    }
}
