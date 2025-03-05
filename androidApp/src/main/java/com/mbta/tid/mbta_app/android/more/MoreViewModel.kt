package com.mbta.tid.mbta_app.android.more

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.model.morePage.localizedFeedbackFormUrl
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoreViewModel(
    private val context: Context,
    private val licensesCallback: () -> Unit,
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

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
        val feedbackFormUrl = run {
            val locales = AppCompatDelegate.getApplicationLocales()
            val primaryLocale = locales[0] ?: context.resources.configuration.locales[0]
            val translation =
                when {
                    primaryLocale.language == "es" -> "es"
                    primaryLocale.language == "fr" -> "fr"
                    primaryLocale.language == "ht" -> "ht"
                    primaryLocale.language == "pt" -> "pt-BR"
                    primaryLocale.language == "vi" -> "vi"
                    primaryLocale.language == "zh" && primaryLocale.script == "Hans" -> "zh-Hans-CN"
                    primaryLocale.language == "zh" && primaryLocale.script == "Hant" -> "zh-Hant-TW"
                    else -> "en"
                }
            localizedFeedbackFormUrl("https://mbta.com/androidappfeedback", translation)
        }
        return listOf(
            MoreSection(
                id = MoreSection.Category.Feedback,
                items =
                    listOf(
                        MoreItem.Link(
                            label = context.resources.getString(R.string.feedback_link_form),
                            url = feedbackFormUrl
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.Resources,
                items =
                    listOf(
                        MoreItem.Link(
                            label =
                                context.resources.getString(R.string.resources_link_trip_planner),
                            url = "https://www.mbta.com/trip-planner"
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.resources_link_fare_info),
                            url = "https://www.mbta.com/fares"
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.resources_link_mticket),
                            note =
                                context.resources.getString(R.string.resources_link_mticket_note),
                            url =
                                "https://play.google.com/store/apps/details?id=com.mbta.mobileapp",
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.Settings,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = context.resources.getString(R.string.setting_toggle_hide_maps),
                            settings = Settings.HideMaps,
                            value = settings[Settings.HideMaps] ?: false
                        ),
                        MoreItem.Toggle(
                            label = context.getString(R.string.setting_elevator_accessibility),
                            settings = Settings.ElevatorAccessibility,
                            value = settings[Settings.ElevatorAccessibility] ?: false
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.FeatureFlags,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = context.getString(R.string.feature_flag_debug_mode),
                            settings = Settings.DevDebugMode,
                            value = settings[Settings.DevDebugMode] ?: false
                        ),
                        MoreItem.Toggle(
                            label = context.getString(R.string.group_by_direction),
                            settings = Settings.GroupByDirection,
                            value = settings[Settings.GroupByDirection] ?: false
                        ),
                        MoreItem.Toggle(
                            label = context.getString(R.string.feature_flag_route_search),
                            settings = Settings.SearchRouteResults,
                            value = settings[Settings.SearchRouteResults] ?: false
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.Other,
                items =
                    listOf(
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_tos),
                            url = "https://www.mbta.com/policies/terms-use"
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_privacy_policy),
                            url = "https://www.mbta.com/policies/privacy-policy"
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_source_code),
                            url = "https://github.com/mbta/mobile_app"
                        ),
                        MoreItem.NavLink(
                            label = context.resources.getString(R.string.software_licenses),
                            callback = licensesCallback
                        )
                    )
            ),
            MoreSection(
                id = MoreSection.Category.Support,
                items = listOf(MoreItem.Phone("617-222-3200", "6172223200")),
                note = context.resources.getString(R.string.more_section_support_note)
            )
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
