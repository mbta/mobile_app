package com.mbta.tid.mbta_app.android.map

import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.MapboxOptions
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

class MapboxConfigManager(private val configUseCase: ConfigUseCase = UsecaseDI().configUsecase) :
    KoinComponent {

    private val _configLoadAttempted = MutableStateFlow(false)
    val configLoadAttempted: StateFlow<Boolean> = _configLoadAttempted
    private val _lastMapboxErrorTimestamp = MutableStateFlow<Instant?>(null)
    var lastMapboxErrorTimestamp = _lastMapboxErrorTimestamp.debounce(1.seconds)

    init {
        HttpServiceFactory.setHttpServiceInterceptor(
            MapHttpInterceptor { _lastMapboxErrorTimestamp.value = Clock.System.now() }
        )
    }

    suspend fun loadConfig() =
        withContext(Dispatchers.IO) {
            val latestConfig = configUseCase.getConfig()
            if (latestConfig is ApiResult.Ok) {
                MapboxOptions.accessToken = latestConfig.data.mapboxPublicToken
            }
            _configLoadAttempted.value = true
        }
}
