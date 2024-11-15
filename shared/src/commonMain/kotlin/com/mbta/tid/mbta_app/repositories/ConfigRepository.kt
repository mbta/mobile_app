package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IConfigRepository {

    suspend fun getConfig(): ApiResult<ConfigResponse>
}

class ConfigRepository : IConfigRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getConfig(): ApiResult<ConfigResponse> =
        ApiResult.runCatching {
            val response = mobileBackendClient.get { url { path("api/protected/config") } }

            return ApiResult.Ok(json.decodeFromString(response.body()))
        }
}

class MockConfigRepository(
    var response: ApiResult<ConfigResponse> =
        ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_mapbox_token"))
) : IConfigRepository, KoinComponent {

    override suspend fun getConfig(): ApiResult<ConfigResponse> {
        return response
    }
}
