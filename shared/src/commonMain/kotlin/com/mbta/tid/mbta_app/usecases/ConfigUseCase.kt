package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import org.koin.core.component.KoinComponent

class ConfigUseCase(
    private val appCheckRepo: IAppCheckRepository,
    private val configRepo: IConfigRepository,
    private val sentryRepo: ISentryRepository
) : KoinComponent {

    suspend fun getConfig(): ApiResult<ConfigResponse> =
        try {
            when (val tokenResult = appCheckRepo.getToken()) {
                is ApiResult.Error -> {
                    sentryRepo.captureMessage(
                        "AppCheck token error ${tokenResult.code} ${tokenResult.message}"
                    )
                    ApiResult.Error(message = "app check token failure ${tokenResult.message}")
                }
                is ApiResult.Ok -> {
                    configRepo.getConfig(tokenResult.data.token)
                }
            }
        } catch (e: Exception) {
            sentryRepo.captureException(e)
            ApiResult.Error(message = e.message ?: e.toString())
        }
}
