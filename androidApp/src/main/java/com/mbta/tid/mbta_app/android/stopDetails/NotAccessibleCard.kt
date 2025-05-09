package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun NotAccessibleCard() {
    if (IsLoadingSheetContents.current) {
        return
    }
    Column(
        modifier = Modifier.haloContainer(2.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.accessibility_icon_not_accessible),
                null,
                modifier =
                    Modifier.size(36.dp)
                        .placeholderIfLoading()
                        .testTag("wheelchair_not_accessible")
                        .clearAndSetSemantics {},
            )
            Text(
                stringResource(R.string.not_accessible_stop_card),
                Modifier.weight(1f),
                style = Typography.callout,
            )
        }
    }
}
