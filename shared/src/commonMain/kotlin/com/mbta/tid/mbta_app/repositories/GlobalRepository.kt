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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface IGlobalRepository {
    public val state: StateFlow<GlobalResponse?>

    public suspend fun getGlobalData(): ApiResult<GlobalResponse>
}

internal class GlobalRepository(
    val cache: ResponseCache<GlobalResponse> =
        ResponseCache.create(cacheKey = "global", invalidationKey = "2025-03-19")
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

public class MockGlobalRepository
@DefaultArgumentInterop.Enabled
constructor(private val result: ApiResult<GlobalResponse>, private val onGet: () -> Unit = {}) :
    IGlobalRepository {

    @DefaultArgumentInterop.Enabled
    public constructor(
        response: GlobalResponse =
            GlobalResponse(
                emptyMap(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
            ),
        onGet: () -> Unit = {},
    ) : this(ApiResult.Ok(response), onGet)

    override val state: MutableStateFlow<GlobalResponse?> =
        MutableStateFlow(
            when (result) {
                is ApiResult.Error -> null
                is ApiResult.Ok -> result.data
            }
        )

    override suspend fun getGlobalData(): ApiResult<GlobalResponse> {
        onGet()
        return result
    }

    internal fun updateGlobalData(newGlobal: GlobalResponse?) {
        state.update { newGlobal }
    }
}

public class IdleGlobalRepository : IGlobalRepository {
    override val state: MutableStateFlow<GlobalResponse?> = MutableStateFlow(null)

    override suspend fun getGlobalData(): ApiResult<GlobalResponse> {
        return suspendCancellableCoroutine {}
    }
}
