package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
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

class GlobalRepository : IGlobalRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getGlobalData(): GlobalResponse =
        mobileBackendClient
            .get {
                timeout { requestTimeoutMillis = 10000 }
                url { path("api/global") }
                expectSuccess = true
            }
            .body()
}

class MockGlobalRepository : IGlobalRepository {
    override suspend fun getGlobalData(): GlobalResponse {
        return GlobalResponse(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

class IdleGlobalRepository : IGlobalRepository {
    override suspend fun getGlobalData(): GlobalResponse {
        return suspendCancellableCoroutine {}
    }
}
