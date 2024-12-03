package com.mbta.tid.mbta_app.android.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.appVariant
import com.mbta.tid.mbta_app.android.component.LabeledSwitch
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.Settings

@Composable
fun MoreSectionView(section: MoreSection, toggleSetting: ((Settings) -> Unit)) {

    val name: String? =
        when (section.id) {
            MoreSection.Category.Settings -> stringResource(id = R.string.more_section_settings)
            MoreSection.Category.FeatureFlags ->
                stringResource(id = R.string.more_section_feature_flags)
            else -> null
        }

    val note: String? = null

    if (!(section.requiresStaging && appVariant != AppVariant.Staging)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (name != null) {
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    if (note != null) {
                        Text(note)
                    }
                }
            }
            Column {
                section.items.mapIndexed { index, item ->
                    when (item) {
                        is MoreItem.Toggle ->
                            ListItem({
                                LabeledSwitch(label = item.label, value = item.value) {
                                    toggleSetting(item.settings)
                                }
                            })
                    }

                    if (index < section.items.size) {
                        androidx.compose.material3.HorizontalDivider()
                    }
                }
            }
        }
    }
}
