package com.mbta.tid.mbta_app.android.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.appVariant
import com.mbta.tid.mbta_app.android.component.LabeledSwitch
import com.mbta.tid.mbta_app.android.util.Typography
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
            MoreSection.Category.Resources -> stringResource(id = R.string.more_section_resources)
            MoreSection.Category.Support -> stringResource(id = R.string.more_section_support)
            else -> null
        }

    val note = section.note

    if (!(section.hiddenOnProd && appVariant == AppVariant.Prod)) {
        Column {
            if (name != null) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Text(
                        name,
                        style = Typography.subheadlineSemibold,
                        modifier = Modifier.semantics { heading() }
                    )

                    if (note != null) {
                        Text(note, style = Typography.footnote)
                    }
                }
            }
            Column(
                modifier =
                    Modifier.clip(MaterialTheme.shapes.medium)
                        .border(1.dp, colorResource(R.color.halo), MaterialTheme.shapes.medium)
                        .background(colorResource(R.color.fill3))
            ) {
                section.items.mapIndexed { index, item ->
                    when (item) {
                        is MoreItem.Toggle ->
                            LabeledSwitch(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                label = item.label,
                                value = item.value
                            ) {
                                toggleSetting(item.settings)
                            }
                        is MoreItem.Link ->
                            MoreLink(
                                item.label,
                                item.url,
                                item.note,
                                isKey = section.id == MoreSection.Category.Feedback
                            )
                        is MoreItem.NavLink ->
                            MoreLink(
                                item.label,
                                item.callback,
                                item.note,
                                isKey = section.id == MoreSection.Category.Feedback
                            )
                        is MoreItem.Phone ->
                            MorePhone(label = item.label, phoneNumber = item.phoneNumber)
                        is MoreItem.Action -> MoreButton(label = item.label, action = item.action)
                    }

                    if (index < section.items.size - 1) {
                        androidx.compose.material3.HorizontalDivider()
                    }
                }
            }
        }
    }
}
