package com.mbta.tid.mbta_app.android.more

import android.content.Context
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoreViewModel(val context: Context, private val settingsRepository: ISettingsRepository) :
    ViewModel() {

    private val _settings = MutableStateFlow<Map<Settings, Boolean>>(mapOf())
    private val _sections = MutableStateFlow<List<MoreSection>>(listOf())
    var sections = _sections.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch { loadSettings() }
    }

    fun toggleSetting(setting: Settings) {
        setSettings(mapOf(setting to !(_settings.value[setting] ?: false)))
    }

    fun getSections(settings: Map<Settings, Boolean>): List<MoreSection> {

        return listOf(
            MoreSection(
                id = MoreSection.Category.Settings,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = context.resources.getString(R.string.setting_toggle_hide_maps),
                            settings = Settings.HideMaps,
                            value = settings[Settings.HideMaps] ?: false
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.FeatureFlags,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = "Debug Mode",
                            settings = Settings.DevDebugMode,
                            value = settings[Settings.DevDebugMode] ?: false
                        ),
                        MoreItem.Toggle(
                            label = "Route Search",
                            settings = Settings.SearchRouteResults,
                            value = settings[Settings.SearchRouteResults] ?: false
                        )
                    )
            ),
        )
    }

    private fun setSettings(settings: Map<Settings, Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.setSettings(settings)
            loadSettings()
        }
    }

    private suspend fun loadSettings() {
        val latestSettings = settingsRepository.getSettings()
        _settings.value = latestSettings
        _sections.value = getSections(latestSettings)
    }
}
