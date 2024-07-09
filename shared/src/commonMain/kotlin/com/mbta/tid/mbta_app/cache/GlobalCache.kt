package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

class GlobalCache : KoinComponent {
    internal var data: GlobalResponse? = null
    internal var dataTimestamp: Instant? = null

    private fun getData(): GlobalResponse? {
        val dataTimestamp = this.dataTimestamp
        if (data == null || dataTimestamp == null) return null
        val now = Clock.System.now()
        return if (now - dataTimestamp < maxAge) {
            data
        } else {
            null
        }
    }

    private fun putData(data: GlobalResponse) {
        this.data = data
        this.dataTimestamp = Clock.System.now()
    }

    suspend fun getOrFetch(fetch: suspend () -> GlobalResponse): GlobalResponse {
        val cachedData = this.getData()
        if (cachedData != null) {
            return cachedData
        }
        val fetchedData = fetch()
        this.putData(fetchedData)
        return fetchedData
    }

    companion object {
        val maxAge = 1.hours
    }
}
