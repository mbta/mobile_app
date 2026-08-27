package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.Window
import com.mbta.tid.mbta_app.model.PresetSelection
import com.mbta.tid.mbta_app.model.PresetWindow
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

@Composable
fun PresetWindowSelector(
    presetRows: List<List<PresetWindow>>,
    selectedPreset: PresetSelection,
    presetsEnabled: Boolean,
    onSelect: (Window) -> Unit,
) {

    // TODO: Accessibility of this segmented control
    Column(modifier = Modifier.selectableGroup()) {
        presetRows.forEachIndexed { rowIndex, windows ->
            Row() {
                windows.forEachIndexed { presetIndex, preset ->
                    val isSelected =
                        selectedPreset is PresetSelection.Preset &&
                            rowIndex == selectedPreset.rowIndex &&
                            presetIndex == selectedPreset.columnIndex
                    PresetButton(
                        enabled = presetsEnabled,
                        isSelected = isSelected,
                        onSelect = { onSelect(preset.window) },
                        label = preset.label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row() {
            val isSelected = selectedPreset is PresetSelection.Custom
            val customWindow = Window.customFromCurrentTime(EasternTimeInstant.now())
            PresetButton(
                enabled = true,
                isSelected = isSelected,
                onSelect = { onSelect(customWindow) },
                label = stringResource(R.string.custom),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PresetButton(
    enabled: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier,
) {
    Button(
        onClick = onSelect,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isSelected) colorResource(R.color.key) else colorResource(R.color.fill1),
                contentColor =
                    if (isSelected) colorResource(R.color.fill3)
                    else colorResource(R.color.text).copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(8.dp),
        modifier =
            modifier.selectable(selected = isSelected, enabled = enabled, onClick = onSelect),
    ) {
        Text(label)
    }
}
