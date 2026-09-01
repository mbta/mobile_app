package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.Window
import kotlinx.datetime.DayOfWeek

public class PresetWindow(
    public val label: String,
    public val window: Window,
) {
    public companion object {

        public fun morningPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.morningDefault(daysOfWeek),
            )

        public fun middayPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.middayDefault(daysOfWeek),
            )

        public fun eveningPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.eveningDefault(daysOfWeek),
            )

        public fun allDayPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.allDayDefault(daysOfWeek),
            )
    }
}

public sealed class PresetSelection {
    public data class Preset(val rowIndex: Int, val columnIndex: Int) : PresetSelection()

    public object Custom : PresetSelection()

    public companion object {
        public fun selectedPresetFromSettings(
            settings: FavoriteSettings.Notifications,
            presetOptions: List<List<PresetWindow>>,
        ): PresetSelection =
            when {
                settings.windows.size == 1 -> {
                    val targetWindow: Window = settings.windows[0]
                    presetOptions
                        .asSequence()
                        .mapIndexedNotNull { rowIndex, presets ->
                            val presetMatchIndex = presets.indexOfFirst {
                                it.window == targetWindow
                            }
                            if (presetMatchIndex != -1) {
                                Preset(rowIndex, presetMatchIndex)
                            } else {
                                null
                            }
                        }
                        .firstOrNull() ?: Custom
                }
                else -> Custom
            }
    }
}
