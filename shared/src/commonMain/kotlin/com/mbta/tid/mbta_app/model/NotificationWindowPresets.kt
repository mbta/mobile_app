package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.Window

public class PresetWindow(
    public val label: String,
    public val window: Window,
    public val selected: Boolean = false,
) {
    public companion object {

        public fun morningPreset(label: String): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.morningDefault,
            )

        public fun middayPreset(label: String): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.middayDefault,
            )

        public fun eveningPreset(label: String): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.eveningDefault,
            )

        public fun allDayPreset(label: String): PresetWindow =
            PresetWindow(
                label = label,
                window = Window.allDayDefault,
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
                settings.windows.isEmpty() -> Preset(1, 0)
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
