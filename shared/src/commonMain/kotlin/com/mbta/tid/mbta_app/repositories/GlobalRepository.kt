package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.cache.ResponseCache
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IGlobalRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getGlobalData(): GlobalResponse
}

class GlobalRepository() : IGlobalRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()
    private val cache: ResponseCache by inject()

    override suspend fun getGlobalData(): GlobalResponse =
        json.decodeFromString(
            cache.getOrFetch { etag: String? ->
                mobileBackendClient.get {
                    timeout { requestTimeoutMillis = 10000 }
                    url { path("api/global") }
                    header(HttpHeaders.IfNoneMatch, etag)
                }
            }
        )
}

class MockGlobalRepository
@DefaultArgumentInterop.Enabled
constructor(
    val response: GlobalResponse =
        GlobalResponse(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap()),
    val onGet: () -> Unit = {}
) : IGlobalRepository {
    override suspend fun getGlobalData(): GlobalResponse {
        onGet()
        return response
    }
}

class IdleGlobalRepository : IGlobalRepository {
    override suspend fun getGlobalData(): GlobalResponse {
        return suspendCancellableCoroutine {}
    }
}
