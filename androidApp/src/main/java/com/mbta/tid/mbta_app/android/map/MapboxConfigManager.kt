package com.mbta.tid.mbta_app.android.map

import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.MapboxOptions
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

interface IMapboxConfigManager {
    val configLoadAttempted: StateFlow<Boolean>
    var lastMapboxErrorTimestamp: Flow<EasternTimeInstant?>

    suspend fun loadConfig()
}

open class MapboxConfigManager(
    private val configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
    val configureMapboxToken: (String) -> Unit = { token -> MapboxOptions.accessToken = token },
    setHttpInterceptor: (MapHttpInterceptor?) -> Unit = { interceptor ->
        HttpServiceFactory.setHttpServiceInterceptor(interceptor)
    },
) : KoinComponent, IMapboxConfigManager {

    private val _configLoadAttempted = MutableStateFlow(false)
    override val configLoadAttempted: StateFlow<Boolean> = _configLoadAttempted
    private val _lastMapboxErrorTimestamp = MutableStateFlow<EasternTimeInstant?>(null)
    override var lastMapboxErrorTimestamp = _lastMapboxErrorTimestamp.debounce(1.seconds)

    init {
        setHttpInterceptor(
            MapHttpInterceptor { _lastMapboxErrorTimestamp.value = EasternTimeInstant.now() }
        )
    }

    override suspend fun loadConfig() =
        withContext(Dispatchers.IO) {
            val latestConfig = configUseCase.getConfig()
            if (latestConfig is ApiResult.Ok) {
                configureMapboxToken(latestConfig.data.mapboxPublicToken)
            }
            _configLoadAttempted.value = true
        }
}
