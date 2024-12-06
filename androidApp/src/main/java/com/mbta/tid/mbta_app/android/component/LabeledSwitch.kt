package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

// From https://www.magentaa11y.com/checklist-native/toggle-switch/
@Composable
fun LabeledSwitch(label: String, value: Boolean, onValueChange: ((Boolean) -> Unit)) {
    Row(
        modifier =
            Modifier.toggleable(
                    value = value,
                    role = Role.Switch,
                    onValueChange = onValueChange,
                )
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)

        Switch(checked = value, onCheckedChange = null)
    }
}
