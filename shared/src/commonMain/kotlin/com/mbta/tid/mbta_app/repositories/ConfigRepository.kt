package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.model.response.ErrorDetails
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IConfigRepository {

    suspend fun getConfig(token: String): ApiResult<ConfigResponse>
}

class ConfigRepository() : IConfigRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getConfig(token: String): ApiResult<ConfigResponse> {
        try {
            val response =
                mobileBackendClient.get {
                    url {
                        path("api/protected/config")
                        header("http_x_firebase_appcheck", token)
                    }
                }

            if (response.status === HttpStatusCode.OK) {
                return ApiResult.Ok(data = json.decodeFromString(response.body()))
            } else {
                return ApiResult.Error(ErrorDetails(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            return ApiResult.Error(ErrorDetails(message = e.message ?: e.toString()))
        }
    }
}

class MockConfigRepository(
    var response: ApiResult<ConfigResponse> =
        ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_mapbox_token"))
) : IConfigRepository, KoinComponent {

    override suspend fun getConfig(token: String): ApiResult<ConfigResponse> {
        return response
    }
}
