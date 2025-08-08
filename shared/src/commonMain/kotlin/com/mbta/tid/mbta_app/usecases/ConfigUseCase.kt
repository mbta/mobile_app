package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import org.koin.core.component.KoinComponent

public class ConfigUseCase(
    private val configRepo: IConfigRepository,
    private val sentryRepo: ISentryRepository,
) : KoinComponent {

    public suspend fun getConfig(): ApiResult<ConfigResponse> =
        try {
            configRepo.getConfig()
        } catch (e: Exception) {
            sentryRepo.captureException(e)
            ApiResult.Error(message = e.message ?: e.toString())
        }
}
