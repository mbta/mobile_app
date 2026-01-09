package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun WithLastTripPrefix(
    last: Boolean,
    alpha: Float,
    scheduleClock: Boolean = false,
    alignment: Alignment.Horizontal = Alignment.End,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp, alignment),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (last) {
            if (scheduleClock) {
                Image(
                    painterResource(R.drawable.fa_clock),
                    null,
                    Modifier.placeholderIfLoading()
                        .padding(4.dp)
                        .size(12.dp)
                        .testTag("lastScheduleIndicator"),
                    alpha = alpha,
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, alignment),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    stringResource(R.string.last),
                    // Hidden from accessibility because the content description in
                    // UpcomingTripView includes it separately
                    Modifier.semantics { hideFromAccessibility() }
                        .placeholderIfLoading()
                        .alpha(alpha)
                        .alignByBaseline(),
                    style = Typography.footnote,
                )
                content()
            }
        } else {
            content()
        }
    }
}
