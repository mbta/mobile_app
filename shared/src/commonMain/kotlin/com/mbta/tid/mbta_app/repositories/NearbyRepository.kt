package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface INearbyRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData
}

class NearbyRepository : KoinComponent, INearbyRepository {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
        val response: NearbyResponse =
            mobileBackendClient
                .get {
                    url {
                        path("api/nearby/")
                        parameters.append("latitude", location.latitude.toString())
                        parameters.append("longitude", location.longitude.toString())
                    }
                    expectSuccess = true
                }
                .body()
        return NearbyStaticData(global, response)
    }
}

class MockNearbyRepository : INearbyRepository {
    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
        return NearbyStaticData(data = emptyList())
    }
}

class IdleNearbyRepository : INearbyRepository {
    override suspend fun getNearby(global: GlobalResponse, location: Coordinate): NearbyStaticData {
        return suspendCancellableCoroutine {}
    }
}
