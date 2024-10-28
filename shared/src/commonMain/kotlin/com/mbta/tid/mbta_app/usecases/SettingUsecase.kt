package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Setting
import com.mbta.tid.mbta_app.repositories.Settings
import org.koin.core.component.KoinComponent

class SettingUsecase(private val repository: ISettingsRepository) : KoinComponent {

    suspend fun get(setting: Settings): Boolean {
        val settings = repository.getSettings()
        return settings.find { it.key == setting }?.isOn ?: false
    }

    suspend fun set(setting: Settings, value: Boolean) {
        val oldSettings = repository.getSettings()
        val newSettings =
            oldSettings.map {
                if (it.key == setting) {
                    Setting(setting, value)
                } else {
                    it
                }
            }
        repository.setSettings(newSettings.toSet())
    }
}
