package com.mbta.tid.mbta_app.android.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.MapboxOptions
import com.mbta.tid.mbta_app.dependencyInjection.UsecaseDI
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface IMapViewModel {
    var lastMapboxErrorTimestamp: Flow<Instant?>

    suspend fun loadConfig()
}

open class MapViewModel(
    private val configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
    val configureMapboxToken: (String) -> Unit = { token -> MapboxOptions.accessToken = token },
    setHttpInterceptor: (MapHttpInterceptor?) -> Unit = { interceptor ->
        HttpServiceFactory.setHttpServiceInterceptor(interceptor)
    }
) : ViewModel(), IMapViewModel {
    private val _config = MutableStateFlow<ApiResult<ConfigResponse>?>(null)
    var config: StateFlow<ApiResult<ConfigResponse>?> = _config
    private val _lastMapboxErrorTimestamp = MutableStateFlow<Instant?>(null)
    override var lastMapboxErrorTimestamp = _lastMapboxErrorTimestamp.debounce(1.seconds)

    init {
        setHttpInterceptor(MapHttpInterceptor { updateLastErrorTimestamp() })
    }

    private fun updateLastErrorTimestamp() {
        _lastMapboxErrorTimestamp.value = Clock.System.now()
    }

    override suspend fun loadConfig() {
        val latestConfig = configUseCase.getConfig()
        if (latestConfig is ApiResult.Ok) {
            configureMapboxToken(latestConfig.data.mapboxPublicToken)
        }
        _config.value = latestConfig
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel() as T
        }
    }
}
