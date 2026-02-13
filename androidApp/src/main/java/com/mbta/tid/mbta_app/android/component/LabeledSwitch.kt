package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import com.mbta.tid.mbta_app.android.util.Typography

@Composable
fun LabeledSwitch(
    modifier: Modifier = Modifier,
    label: String,
    value: Boolean,
    enabled: Boolean = true,
    onValueChange: (Boolean) -> Unit,
) {
    LabeledSwitch(
        modifier,
        { Text(text = label, style = Typography.body, modifier = Modifier.weight(1f)) },
        value,
        enabled,
        onValueChange,
    )
}

// From https://www.magentaa11y.com/checklist-native/toggle-switch/
@Composable
fun LabeledSwitch(
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
    value: Boolean,
    enabled: Boolean = true,
    onValueChange: ((Boolean) -> Unit),
) {
    Row(
        modifier =
            Modifier.then(
                    if (enabled)
                        Modifier.toggleable(
                            value = value,
                            role = Role.Switch,
                            onValueChange = onValueChange,
                        )
                    else Modifier
                )
                .alpha(if (enabled) 1.0f else 0.6f)
                .fillMaxWidth()
                .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        label()

        Switch(checked = value, onCheckedChange = null, enabled = enabled)
    }
}
