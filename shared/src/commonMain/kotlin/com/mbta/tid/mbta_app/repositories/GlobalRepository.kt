package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.cache.ResponseCache
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IGlobalRepository {
    val state: StateFlow<GlobalResponse?>

    suspend fun getGlobalData(): ApiResult<GlobalResponse>
}

class GlobalRepository(
    val cache: ResponseCache<GlobalResponse> = ResponseCache.create(cacheKey = "global")
) : IGlobalRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()
    override val state = cache.state

    override suspend fun getGlobalData() =
        cache.getOrFetch { etag: String? ->
            mobileBackendClient.get {
                timeout { requestTimeoutMillis = 10000 }
                url { path("api/global") }
                header(HttpHeaders.IfNoneMatch, etag)
            }
        }
}

class MockGlobalRepository
@DefaultArgumentInterop.Enabled
constructor(
    val response: GlobalResponse =
        GlobalResponse(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap()),
    val onGet: () -> Unit = {}
) : IGlobalRepository {
    override val state = MutableStateFlow(response)

    override suspend fun getGlobalData(): ApiResult<GlobalResponse> {
        onGet()
        return ApiResult.Ok(response)
    }
}

class IdleGlobalRepository : IGlobalRepository {
    override val state = MutableStateFlow(null)

    override suspend fun getGlobalData(): ApiResult<GlobalResponse> {
        return suspendCancellableCoroutine {}
    }
}
