package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import org.koin.core.component.KoinComponent

class GetSettingUsecase(private val repository: ISettingsRepository) : KoinComponent {

    suspend fun execute(setting: Settings): Boolean {
        val settings = repository.getSettings()
        return settings.find { it.key == setting }?.isOn ?: false
    }
}
