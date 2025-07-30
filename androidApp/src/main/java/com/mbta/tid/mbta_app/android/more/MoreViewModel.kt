package com.mbta.tid.mbta_app.android.more

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.model.morePage.localizedFeedbackFormUrl
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MoreViewModel(private val context: Context, private val licensesCallback: () -> Unit) :
    ViewModel() {

    private val _sections = MutableStateFlow<List<MoreSection>>(listOf())
    var sections = _sections.asStateFlow()

    init {
        _sections.value = getSections()
    }

    fun getSections(): List<MoreSection> {
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
                            url = feedbackFormUrl,
                        )
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Resources,
                items =
                    listOf(
                        MoreItem.Link(
                            label =
                                context.resources.getString(R.string.resources_link_trip_planner),
                            url = "https://www.mbta.com/trip-planner",
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.resources_link_fare_info),
                            url = "https://www.mbta.com/fares",
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.resources_link_mticket),
                            note =
                                context.resources.getString(R.string.resources_link_mticket_note),
                            url = "https://play.google.com/store/apps/details?id=com.mbta.mobileapp",
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Settings,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label =
                                context.resources.getString(R.string.setting_toggle_map_display),
                            settings = Settings.HideMaps,
                        ),
                        MoreItem.Toggle(
                            label = context.getString(R.string.setting_station_accessibility),
                            settings = Settings.StationAccessibility,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.FeatureFlags,
                items =
                    listOf(
                        MoreItem.Toggle(
                            label = context.getString(R.string.feature_flag_debug_mode),
                            settings = Settings.DevDebugMode,
                        ),
                        MoreItem.Toggle(
                            label = "Enhanced Favorites",
                            settings = Settings.EnhancedFavorites,
                        ),
                        MoreItem.Toggle(
                            label = context.getString(R.string.feature_flag_route_search),
                            settings = Settings.SearchRouteResults,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Other,
                items =
                    listOf(
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_tos),
                            url = "https://www.mbta.com/policies/terms-use",
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_privacy_policy),
                            url = "https://www.mbta.com/policies/privacy-policy",
                        ),
                        MoreItem.Link(
                            label = context.resources.getString(R.string.other_link_source_code),
                            url = "https://github.com/mbta/mobile_app",
                        ),
                        MoreItem.NavLink(
                            label = context.resources.getString(R.string.software_licenses),
                            callback = licensesCallback,
                        ),
                    ),
            ),
            MoreSection(
                id = MoreSection.Category.Support,
                items = listOf(MoreItem.Phone("617-222-3200", "6172223200")),
                noteAbove = context.getString(R.string.more_section_support_note_hours),
                noteBelow = context.getString(R.string.more_section_support_note_accessibility),
            ),
        )
    }
}
