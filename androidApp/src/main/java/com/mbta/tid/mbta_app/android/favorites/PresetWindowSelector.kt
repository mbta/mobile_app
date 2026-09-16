package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.formattedHour
import com.mbta.tid.mbta_app.model.Preset
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

@Composable
fun PresetWindowSelector(
    presetRows: List<List<Preset>>,
    selectedPreset: Preset?,
    onSelect: (Preset?) -> Unit,
) {
    val maxColumnCount = presetRows.firstOrNull()?.size ?: 0

    Column(
        modifier =
            Modifier.fillMaxWidth().semantics {
                collectionInfo =
                    CollectionInfo(
                        rowCount = presetRows.size + 1, // +1 for Custom
                        columnCount = maxColumnCount,
                    )
            }
    ) {
        presetRows.forEachIndexed { rowIndex, windows ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                windows.forEachIndexed { presetIndex, preset ->
                    val isSelected = selectedPreset == preset

                    val description =
                        if (preset == Preset.AllDay) {
                            stringResource(R.string.start_to_end_of_service)
                        } else {
                            stringResource(
                                R.string.start_to_end_time,
                                EasternTimeInstant(
                                        EasternTimeInstant.now().local.date,
                                        preset.startTime,
                                    )
                                    .formattedHour(),
                                EasternTimeInstant(
                                        EasternTimeInstant.now().local.date,
                                        preset.endTime,
                                    )
                                    .formattedHour(),
                            )
                        }
                    PresetButton(
                        isSelected = isSelected,
                        onSelect = { onSelect(preset) },
                        label =
                            when (preset) {
                                Preset.Morning -> stringResource(R.string.morning)
                                Preset.Midday -> stringResource(R.string.midday)
                                Preset.Evening -> stringResource(R.string.evening)
                                Preset.AllDay -> stringResource(R.string.all_day)
                            },
                        description = description,
                        centerContent = preset != Preset.AllDay,
                        modifier =
                            Modifier.weight(1f).semantics {
                                collectionItemInfo =
                                    CollectionItemInfo(
                                        rowIndex = rowIndex,
                                        rowSpan = 1,
                                        columnIndex = presetIndex,
                                        columnSpan = 1,
                                    )
                            },
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            val isSelected = selectedPreset == null
            PresetButton(
                isSelected = isSelected,
                onSelect = { onSelect(null) },
                label = stringResource(R.string.custom),
                description = "…",
                centerContent = false,
                modifier =
                    Modifier.weight(1f).semantics {
                        collectionItemInfo =
                            CollectionItemInfo(
                                rowIndex = presetRows.size,
                                rowSpan = 1,
                                columnIndex = 0,
                                columnSpan = maxColumnCount,
                            )
                    },
            )
        }
    }
}

@Composable
fun PresetButton(
    isSelected: Boolean,
    onSelect: () -> Unit,
    label: String,
    description: String,
    centerContent: Boolean,
    modifier: Modifier,
) {
    val contentModifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)

    val trailingContent: @Composable () -> Unit = {
        Row(modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    painterResource(R.drawable.fa_bell_filled),
                    null,
                )
            } else {
                Text(description, style = Typography.footnote)
            }
        }
    }

    Button(
        onClick = onSelect,
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isSelected) colorResource(R.color.key) else colorResource(R.color.fill3),
                contentColor =
                    if (isSelected) colorResource(R.color.fill3)
                    else colorResource(R.color.text).copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier =
            modifier.selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.Tab,
            ),
    ) {
        if (centerContent) {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = Typography.bodySemibold)
                trailingContent()
            }
        } else {
            Row(
                modifier = contentModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = Typography.bodySemibold)
                trailingContent()
            }
        }
    }
}
