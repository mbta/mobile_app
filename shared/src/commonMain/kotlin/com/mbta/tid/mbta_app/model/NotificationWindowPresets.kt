package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.Window
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

public class PresetWindow(
    public val label: String,
    public val window: Window,
) {
    public companion object {

        public fun morningPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window(Preset.Morning, daysOfWeek),
            )

        public fun middayPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window(Preset.Midday, daysOfWeek),
            )

        public fun eveningPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window(Preset.Evening, daysOfWeek),
            )

        public fun allDayPreset(label: String, daysOfWeek: Set<DayOfWeek>): PresetWindow =
            PresetWindow(
                label = label,
                window = Window(Preset.AllDay, daysOfWeek),
            )
    }
}

public sealed class PresetSelection {
    public data class Preset(val rowIndex: Int, val columnIndex: Int) : PresetSelection()

    public object Custom : PresetSelection()

    public companion object {
        public fun selectedPresetFromWindows(
            windows: List<Window>,
            presetOptions: List<List<PresetWindow>>,
        ): PresetSelection =
            when {
                windows.size == 1 -> {
                    val targetWindow: Window = windows[0]
                    presetOptions
                        .asSequence()
                        .mapIndexedNotNull { rowIndex, presets ->
                            val presetMatchIndex = presets.indexOfFirst {
                                it.window == targetWindow
                            }
                            if (presetMatchIndex != -1) {
                                PresetSelection.Preset(rowIndex, presetMatchIndex)
                            } else {
                                null
                            }
                        }
                        .firstOrNull() ?: PresetSelection.Custom
                }
                else -> {
                    PresetSelection.Custom
                }
            }
    }
}

public enum class Preset(public val startTime: LocalTime, public val endTime: LocalTime) {
    Morning(LocalTime(6, 0), LocalTime(10, 0)),
    Midday(LocalTime(10, 0), LocalTime(16, 0)),
    Evening(LocalTime(16, 0), LocalTime(20, 0)),
    AllDay(LocalTime(0, 0), LocalTime(23, 59));

    public companion object {

        public fun selected(windows: List<Window>): Preset? {
            if (windows.size == 1) {
                val targetWindow: Window = windows[0]
                val presetMatch =
                    Preset.entries.firstOrNull {
                        it.startTime == targetWindow.startTime &&
                            it.endTime == targetWindow.endTime &&
                            (targetWindow.daysOfWeek == Window.weekend ||
                                targetWindow.daysOfWeek == Window.weekdays)
                    }
                return presetMatch
            }
            return null
        }
    }
}
