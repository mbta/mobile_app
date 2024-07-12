package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.model.response.ErrorDetails
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import org.koin.core.component.KoinComponent

interface IConfigUseCase {
    suspend fun getConfig(): ApiResult<ConfigResponse>
}

class ConfigUseCase(
    private val appCheckRepo: IAppCheckRepository,
    private val configRepo: IConfigRepository
) : IConfigUseCase, KoinComponent {

    override suspend fun getConfig(): ApiResult<ConfigResponse> {
        try {
            when (val tokenResult = appCheckRepo.getToken()) {
                is ApiResult.Error ->
                    return ApiResult.Error(
                        ErrorDetails(
                            message = "app check token failure ${tokenResult.error.message}"
                        )
                    )
                is ApiResult.Ok -> {
                    return configRepo.getConfig(tokenResult.data.token)
                }
            }
        } catch (e: Exception) {
            return ApiResult.Error(ErrorDetails(message = e.message ?: e.toString()))
        }
    }
}
